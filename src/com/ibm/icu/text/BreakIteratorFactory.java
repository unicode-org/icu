/*
 *******************************************************************************
 * Copyright (C) 2002-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/BreakIteratorFactory.java,v $ 
 * $Date: 2004/01/26 23:04:28 $ 
 * $Revision: 1.9 $
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
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.util.ULocale;

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
        iter.setText(new java.text.StringCharacterIterator(""));
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
        Locale[] actualLoc = new Locale[1];
        BreakIterator iter = (BreakIterator)service.get(locale, kind, actualLoc);
        ULocale uloc = new ULocale(actualLoc[0]);
        iter.setLocale(uloc, uloc); // services make no distinction between actual & valid
        return iter;
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

        BreakIterator iter = null;

        if (classNames[kind].equals("RuleBasedBreakIterator")) {
            iter = new RuleBasedBreakIterator(rules);
        }
        else if (classNames[kind].equals("DictionaryBasedBreakIterator")) {
            try {
                // System.out.println(dictionaryName);
                Object t = bundle.getObject(dictionaryName);
                // System.out.println(t);
                URL url = (URL)t;
                InputStream dictionary = url.openStream();
                iter = new DictionaryBasedBreakIterator(rules, dictionary);
            }
            catch(IOException e) {
            }
            catch(MissingResourceException e) {
            }
	    // TODO: we don't have 'bad' resource data, so this should never happen
	    // in our current tests.
	    ///CLOVER:OFF
            if (iter == null) {
                iter = new RuleBasedBreakIterator(rules);
            }
	    ///CLOVER:ON
        }
        else {
	    // TODO: we don't have 'bad' resource data, so this should never happen 
	    // in our current tests.
	    ///CLOVER:OFF
            throw new IllegalArgumentException("Invalid break iterator class \"" +
                            classNames[kind] + "\"");
	    ///CLOVER:ON
        }

        // TODO: Determine valid and actual locale correctly.
        ULocale uloc = new ULocale(bundle.getLocale());
        iter.setLocale(uloc, uloc);
        return iter;
    }
}
