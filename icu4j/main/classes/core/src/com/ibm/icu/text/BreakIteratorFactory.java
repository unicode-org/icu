// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2002-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.MissingResourceException;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.ICUBinary;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICUResourceBundle;
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

    @Override
    public Object registerInstance(BreakIterator iter, ULocale locale, int kind) {
        iter.setText(new java.text.StringCharacterIterator(""));
        return service.registerObject(iter, locale, kind);
    }

    @Override
    public boolean unregister(Object key) {
        if (service.isDefault()) {
            return false;
        }
        return service.unregisterFactory((Factory)key);
    }

    @Override
    public Locale[] getAvailableLocales() {
        if (service == null) {
            return ICUResourceBundle.getAvailableLocales();
        } else {
            return service.getAvailableLocales();
        }
    }

    @Override
    public ULocale[] getAvailableULocales() {
        if (service == null) {
            return ICUResourceBundle.getAvailableULocales();
        } else {
            return service.getAvailableULocales();
        }
    }

    @Override
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
                @Override
                protected Object handleCreate(ULocale loc, int kind, ICUService srvc) {
                    return createBreakInstance(loc, kind);
                }
            }
            registerFactory(new RBBreakIteratorFactory());

            markDefault();
        }

        /**
         * createBreakInstance() returns an appropriate BreakIterator for any locale.
         * It falls back to root if there is no specific data.
         *
         * <p>Without this override, the service code would fall back to the default locale
         * which is not desirable for an algorithm with a good Unicode default,
         * like break iteration.
         */
        @Override
        public String validateFallbackLocale() {
            return "";
        }
    }
    static final ICULocaleService service = new BFService();


    /** KIND_NAMES are the resource key to be used to fetch the name of the
     *             pre-compiled break rules.  The resource bundle name is "boundaries".
     *             The value for each key will be the rules to be used for the
     *             specified locale - "word" -> "word_th" for Thai, for example.
     */
    private static final String[] KIND_NAMES = {
            "grapheme", "word", "line", "sentence", "title"
    };


    private static BreakIterator createBreakInstance(ULocale locale, int kind) {

        RuleBasedBreakIterator    iter = null;
        ICUResourceBundle rb           = ICUResourceBundle.
                getBundleInstance(ICUData.ICU_BRKITR_BASE_NAME, locale,
                        ICUResourceBundle.OpenType.LOCALE_ROOT);

        //
        //  Get the binary rules.
        //
        ByteBuffer bytes = null;
        String typeKeyExt = "";
        if (kind == BreakIterator.KIND_LINE) {
            String keyValue = locale.getKeywordValue("lb");
            if ( keyValue != null && (keyValue.equals("strict") || keyValue.equals("normal") || keyValue.equals("loose")) ) {
                typeKeyExt = "_" + keyValue;
            }
            String language = locale.getLanguage();
            if (language != null && language.equals("ja")) {
                keyValue = locale.getKeywordValue("lw");
                if (keyValue != null && keyValue.equals("phrase")) {
                    typeKeyExt += "_" + keyValue;
                }
            }
        }

        String brkfname;
        try {
            String         typeKey       = typeKeyExt.isEmpty() ? KIND_NAMES[kind] : KIND_NAMES[kind] + typeKeyExt;
                           brkfname      = rb.getStringWithFallback("boundaries/" + typeKey);
            String         rulesFileName = ICUData.ICU_BRKITR_NAME+ '/' + brkfname;
                           bytes         = ICUBinary.getData(rulesFileName);
        }
        catch (Exception e) {
            throw new MissingResourceException(e.toString(),"","");
        }

        //
        // Create a normal RuleBasedBreakIterator.
        //
        try {
            boolean isPhraseBreaking = (brkfname != null) && brkfname.contains("phrase");
            iter = RuleBasedBreakIterator.getInstanceFromCompiledRules(bytes, isPhraseBreaking);
        }
        catch (IOException e) {
            // Shouldn't be possible to get here.
            // If it happens, the compiled rules are probably corrupted in some way.
            Assert.fail(e);
        }
        // TODO: Determine valid and actual locale correctly.
        ULocale uloc = ULocale.forLocale(rb.getLocale());
        iter.setLocale(uloc, uloc);

        // filtered break
        if (kind == BreakIterator.KIND_SENTENCE) {
            final String ssKeyword = locale.getKeywordValue("ss");
            if (ssKeyword != null && ssKeyword.equals("standard")) {
                final ULocale base = new ULocale(locale.getBaseName());
                return FilteredBreakIteratorBuilder.getInstance(base).wrapIteratorWithFilter(iter);
            }
        }

        return iter;

    }

}
