/*
 *******************************************************************************
 * Copyright (C) 2002-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

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

    // KIND_NAMES are used in synthesizing the resource name that holds the source
    //            break rules.   For old-style (ICU 2.8 and previous) break iterators.
    //            The resources are com.ibm.icu.impl.data.BreakIteratorRules, and have
    //            names like "CharacterBreakRules", where the "Character" part of the
    //            name comes from here (this array).
    private static final String[] KIND_NAMES = {
        "Character", "Word", "Line", "Sentence", "Title"
    };

    /** KIND_NAMES_2 are used in synthesizing the names for
     *  the precompiled break rules used with the new (ICU 3.0) RBBI.
     *  The fully assembled names look like icudt30b_char.brk, which is the
     *  file name of the brk file as produced by the ICU4C build.
     *  @internal
     */
    private static final String[] KIND_NAMES_2 = {
            "char", "word", "line", "sent", "title"
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

		BreakIterator iter = null;
        ResourceBundle bundle = ICULocaleData.getResourceBundle("BreakIteratorRules", where);
        String[] classNames = bundle.getStringArray("BreakIteratorClasses");
        String rules = bundle.getString(rulesName);
        if (classNames[kind].equals("RuleBasedBreakIterator")) {
            // Old style (2.8 and previous) Break Iterator.
            // Not used by default, but if someone wants to specify the old class
            //   in some locale's resources, it should still work.
            iter = new RuleBasedBreakIterator_Old(rules);
        }
        else if (classNames[kind].equals("RuleBasedBreakIterator_New")) {
        	try {
        		// Class for new RBBI engine.
        		// Open a stream to the .brk file.  Path to the brk files has this form:
        		//      data/icudt30b/line.brk      (30 is version number)
        		String rulesFileName = 
        			"data/icudt" + VersionInfo.ICU_VERSION.getMajor() +  
					VersionInfo.ICU_VERSION.getMinor() + "b/" + 
					KIND_NAMES_2[kind] + ".brk";
        		InputStream is = ICUData.getStream(rulesFileName);
        		if (is == null) {
        			// Temporary!!! Try again with break files named data/icudt28b_char.brk
        			//              (or word, line, etc.)   This was a temporary location
        			//              used during development, this code can be removed once
        			//              the data is in the data directory, above.  TODO:  remove 
        			//              the following code, make this catch turn around and throw.
        			rulesFileName = 
        				"data/icudt" + VersionInfo.ICU_VERSION.getMajor() +  
						VersionInfo.ICU_VERSION.getMinor() + "b_" + 
						KIND_NAMES_2[kind] + ".brk";
        			is = ICUData.getRequiredStream(rulesFileName);
        		}
        		iter = RuleBasedBreakIterator_New.getInstanceFromCompiledRules(is);   
        	}
        	catch (IOException e) {
        		throw    new IllegalArgumentException(e.toString());
        	}
        }
        else if (classNames[kind].equals("DictionaryBasedBreakIterator")) {
            try {
				InputStream dictionary = ICUData.getStream(bundle.getString(dictionaryName));
//                System.out.println("bundle: " + bundle + " dn: " + dictionaryName);
//                Object t = bundle.getObject(dictionaryName);
//                // System.out.println(t);
//                URL url = (URL)t;
//                System.out.println("url: " + url);
//                InputStream dictionary = url.openStream();
//                System.out.println("stream: " + dictionary);
                iter = new DictionaryBasedBreakIterator(rules, dictionary);
            }
            catch(IOException e) {
            	System.out.println(e); // debug
            }
            catch(MissingResourceException e) {
            	System.out.println(e); // debug
            }
	    // TODO: we don't have 'bad' resource data, so this should never happen
	    // in our current tests.
	    ///CLOVER:OFF
            if (iter == null) {
                iter = new RuleBasedBreakIterator_Old(rules);
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
