/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/BreakIteratorFactory.java,v $ 
 * $Date: 2003/05/05 23:42:17 $ 
 * $Revision: 1.3 $
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
final class BreakIteratorFactory extends BreakIterator.BreakIteratorServiceShim {

    public Object registerInstance(BreakIterator iter, Locale locale, int kind) {
        return service.registerObject(iter, locale, kind);
    }
    
    public boolean unregister(Object key) {
        if (service.isDefault()) {
            return false;
        }
        return service.unregisterFactory((Factory)key);
    } 
    
    public Locale[] getAvailableLocales() {
        if (service == null) {
            return ICULocaleData.getAvailableLocales();
        } else {
            return service.getAvailableLocales();
        }
    }
    
    public BreakIterator createBreakIterator(Locale locale, int kind) {
        if (service.isDefault()) {
            return createBreakInstance(locale, kind);
        }
        return (BreakIterator)service.get(locale, kind);
    }

    private static class BFService extends ICULocaleService {
        BFService() {
            super("BreakIterator");

            class RBBreakIteratorFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(Locale loc, int kind, ICUService service) {
                    return createBreakInstance(loc, kind);
                }
            }
            registerFactory(new RBBreakIteratorFactory());

            markDefault();
        }
    }
    static final ICULocaleService service = new BFService();

    private static final String[] KIND_NAMES = {
        "Character", "Word", "Line", "Sentence", "Title"
    };

    private static BreakIterator createBreakInstance(Locale locale, int kind) {
        String prefix = KIND_NAMES[kind];
        return createBreakInstance(locale, kind, 
                                   prefix + "BreakRules", 
                                   prefix + "BreakDictionary");
    }

    private static BreakIterator createBreakInstance(Locale where,
                                             int kind,
                                             String rulesName,
                                             String dictionaryName) {

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
        else {
            throw new IllegalArgumentException("Invalid break iterator class \"" +
                            classNames[kind] + "\"");
        }
    }
}
