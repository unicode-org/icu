/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/BreakIteratorFactory.java,v $ 
 * $Date: 2003/01/28 18:55:41 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICULocaleService.LocaleKey;
import com.ibm.icu.impl.ICULocaleService.ICUResourceBundleFactory;
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.impl.ICUService.Key;
import com.ibm.icu.impl.LocaleUtility;


/**
 * @author Ram
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
class BreakIteratorFactory {
    private static ICULocaleService service;
    static ICULocaleService getService() {
        if (service == null) {
            ICULocaleService newService = new ICULocaleService("BreakIterator");

            class RBBreakIteratorFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(Locale loc, int kind, ICUService service) {
                    return createBreakInstance(loc, kind);
                }
            }
            newService.registerFactory(new RBBreakIteratorFactory());

            synchronized (BreakIterator.class) {
                if (service == null) {
                    service = newService;
                }
            }
        }
        return service;
    }
    static boolean unregisterFactory(Object key) {
        if (service != null) {
            return service.unregisterFactory((Factory)key);
        }
        return false;
    } 
    
    static synchronized Locale[] getAvailableLocales()
    {
        if (service == null) {
            return ICULocaleData.getAvailableLocales();
        } else {
            return service.getAvailableLocales();
        }
    }
    
    static Object registerInstance(BreakIterator iter, Locale locale, int kind){
        return getService().registerObject(iter, locale, kind);
    }
    
    static BreakIterator createBreakInstance(Locale where,
                                             int kind,
                                             String rulesName,
                                             String dictionaryName) {

//      System.out.println("rulesName: "+rulesName);
//      System.out.println("dictionaryName: "+dictionaryName);
        ResourceBundle bundle = ICULocaleData.getResourceBundle("BreakIteratorRules", where);
        String[] classNames = bundle.getStringArray("BreakIteratorClasses");

        String rules = bundle.getString(rulesName);

        if (classNames[kind].equals("RuleBasedBreakIterator")) {
            return new RuleBasedBreakIterator(rules);
        }
        else if (classNames[kind].equals("DictionaryBasedBreakIterator")) {
            try {
                // System.out.println(dictionaryName);
                Object t = bundle.getObject(dictionaryName);
                // System.out.println(t);
                URL url = (URL)t;
                InputStream dictionary = url.openStream();
                return new DictionaryBasedBreakIterator(rules, dictionary);
            }
            catch(IOException e) {
            }
            catch(MissingResourceException e) {
            }
            return new RuleBasedBreakIterator(rules);
        }
        else
            throw new IllegalArgumentException("Invalid break iterator class \"" +
                            classNames[kind] + "\"");
    }
    private static final String[] KIND_NAMES = {
        "Character", "Word", "Line", "Sentence", "Title"
    };

    static BreakIterator createBreakInstance(Locale locale, int kind) {
        String prefix = KIND_NAMES[kind];

        return createBreakInstance( locale, kind, 
                                    prefix + "BreakRules", 
                                    prefix + "BreakDictionary" );
    }
}
