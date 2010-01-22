/*
 *******************************************************************************
 * Copyright (C) 2002-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.IOException;
import java.io.InputStream;

import java.util.Locale;
import java.util.MissingResourceException;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.impl.Assert;

/**
 * @author Ram
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
final class BreakIteratorFactory extends BreakIterator.BreakIteratorServiceShim {

    public Object registerInstance(BreakIterator iter, ULocale locale, int kind) {
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
            return ICUResourceBundle.getAvailableLocales();
        } else {
            return service.getAvailableLocales();
        }
    }

    public ULocale[] getAvailableULocales() {
        if (service == null) {
            return ICUResourceBundle.getAvailableULocales();
        } else {
            return service.getAvailableULocales();
        }
    }

    public BreakIterator createBreakIterator(ULocale locale, int kind) {
    // TODO: convert to ULocale when service switches over
        if (service.isDefault()) {
            return createBreakInstance(locale, kind);
        }
        ULocale[] actualLoc = new ULocale[1];
        BreakIterator iter = (BreakIterator)service.get(locale, kind, actualLoc);
        iter.setLocale(actualLoc[0], actualLoc[0]); // services make no distinction between actual & valid
        return iter;
    }

    private static class BFService extends ICULocaleService {
        BFService() {
            super("BreakIterator");

            class RBBreakIteratorFactory extends ICUResourceBundleFactory {
                protected Object handleCreate(ULocale loc, int kind, ICUService srvc) {
                    return createBreakInstance(loc, kind);
                }
            }
            registerFactory(new RBBreakIteratorFactory());

            markDefault();
        }
    }
    static final ICULocaleService service = new BFService();


    /** KIND_NAMES are the resource key to be used to fetch the name of the
     *             pre-compiled break rules.  The resource bundle name is "boundaries".
     *             The value for each key will be the rules to be used for the
     *             specified locale - "word" -> "word_th" for Thai, for example.
     *  DICTIONARY_POSSIBLE indexes in the same way, and indicates whether a
     *             dictionary is a possibility for that type of break.  This is just
     *             an optimization to avoid a resource lookup where no dictionary is
     *             ever possible.
     */
    private static final String[] KIND_NAMES = {
            "grapheme", "word", "line", "sentence", "title"
        };
    private static final boolean[] DICTIONARY_POSSIBLE = {
            false,      true,  true,   false,     false
    };


    private static BreakIterator createBreakInstance(ULocale locale, int kind) {

        BreakIterator    iter       = null;
        ICUResourceBundle rb        = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BRKITR_BASE_NAME, locale);
        
        //
        //  Get the binary rules.  These are needed for both normal RulesBasedBreakIterators
        //                         and for Dictionary iterators.
        //
        InputStream      ruleStream = null;
        try {
            String         typeKey       = KIND_NAMES[kind];
            String         brkfname      = rb.getStringWithFallback("boundaries/" + typeKey);
            String         rulesFileName = ICUResourceBundle.ICU_BUNDLE +ICUResourceBundle.ICU_BRKITR_NAME+ "/" + brkfname;
                           ruleStream    = ICUData.getStream(rulesFileName);
        }
        catch (Exception e) {
            throw new MissingResourceException(e.toString(),"","");
        }
 
        //
        //  Check whether a dictionary exists, and create a DBBI iterator is
        //   one does.
        //
        if (DICTIONARY_POSSIBLE[kind]) {
            // This type of break iterator could potentially use a dictionary.
            //
            try {
                if (locale.getLanguage().equals("th")){
                    // If the language is Thai, load the thai compact trie dictionary.
                    String dictType = "Thai";
                    String dictFileName = rb.getStringWithFallback("dictionaries/" + dictType);
                    dictFileName = ICUResourceBundle.ICU_BUNDLE +ICUResourceBundle.ICU_BRKITR_NAME+ "/" + dictFileName;
                    InputStream is = ICUData.getStream(dictFileName);
                    iter = new ThaiBreakIterator(ruleStream, is);
                }
            } catch (MissingResourceException e) {
                //  Couldn't find a dictionary.
                //  This is normal, and will occur whenever creating a word or line
                //  break iterator for a locale that does not have a BreakDictionaryData
                //  resource - meaning for all but Thai.
                //  Fall through to creating a normal RulebasedBreakIterator.
            } catch (IOException e) {
                Assert.fail(e);
            }
         }

        if (iter == null) {
            //
            // Create a normal RuleBasedBreakIterator.
            //    We have determined that this is not supposed to be a dictionary iterator.
            //
            try {
                iter = RuleBasedBreakIterator.getInstanceFromCompiledRules(ruleStream);
            }
            catch (IOException e) {
                // Shouldn't be possible to get here.
                // If it happens, the compiled rules are probably corrupted in some way.
                Assert.fail(e);
           }
        }
        // TODO: Determine valid and actual locale correctly.
        ULocale uloc = ULocale.forLocale(rb.getLocale());
        iter.setLocale(uloc, uloc);
        
        return iter;

    }

}
