/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.ULocale;

public class ICULocale {
    private static final String SPI_PROP_FILE = "com/ibm/icu/impl/javaspi/ICUProvider.properties";
    private static final String ADD_VARIANTS_KEY = "com.ibm.icu.impl.javaspi.ICULocale.addIcuVariants";
    private static final String ADD_ISO3_LANG_KEY = "com.ibm.icu.impl.javaspi.ICULocale.addIso3Languages";
    private static final String SUFFIX_KEY = "com.ibm.icu.impl.javaspi.ICULocale.icuVariantSuffix";

    private static final String DEFAULT_SUFFIX = "ICU";
    private static final boolean DEFAULT_ADD_VARIANTS = true;
    private static final boolean DEFAULT_ADD_ISO3_LANG = true;

    private static final Locale[] SPECIAL_LOCALES = {
        new Locale("ja", "JP", "JP"),
        new Locale("no"),
        new Locale("no", "NO"),
        new Locale("no", "NO", "NY"),
        new Locale("sr", "CS"),
        new Locale("th", "TH", "TH"),
    };

    private static Locale[] LOCALES = null;
    private static Boolean ADD_VARIANTS = null;
    private static Boolean ADD_ISO3_LANG = null;
    private static String SUFFIX = null;

    public static Locale[] getAvailableLocales() {
        Locale[] all = getLocales();
        return Arrays.copyOf(all, all.length);
    }

    public static Locale canonicalize(Locale locale) {
        Locale result = locale;
        String variant = locale.getVariant();
        String suffix = getIcuSuffix();
        if (variant.equals(suffix)) {
            result = new Locale(locale.getLanguage(), locale.getCountry());
        } else if (variant.endsWith(suffix)
                && variant.charAt(variant.length() - suffix.length() - 1) == '_') {
            variant = variant.substring(0, variant.length() - suffix.length() - 1);
            result = new Locale(locale.getLanguage(), locale.getCountry(), variant);
        }
        return result;
    }

    private static final Locale THAI_NATIVE_DIGIT_LOCALE = new Locale("th", "TH", "TH");
    private static final char THAI_NATIVE_ZERO = '\u0E50';
    private static DecimalFormatSymbols THAI_NATIVE_DECIMAL_SYMBOLS = null;

    /*
     * Returns a DecimalFormatSymbols if the given locale requires
     * non-standard symbols, more specifically, native digits used
     * by JDK Locale th_TH_TH.  If the locale does not requre a special
     * symbols, null is returned.
     */
    public static synchronized DecimalFormatSymbols getDecimalFormatSymbolsForLocale(Locale loc) {
        if (loc.equals(THAI_NATIVE_DIGIT_LOCALE)) {
            if (THAI_NATIVE_DECIMAL_SYMBOLS == null) {
                THAI_NATIVE_DECIMAL_SYMBOLS = new DecimalFormatSymbols(new ULocale("th_TH"));
                THAI_NATIVE_DECIMAL_SYMBOLS.setDigit(THAI_NATIVE_ZERO);
            }
            return (DecimalFormatSymbols)THAI_NATIVE_DECIMAL_SYMBOLS.clone();
        }
        return null;
    }

    private static synchronized Locale[] getLocales() {
        if (LOCALES != null) {
            return LOCALES;
        }

        Set<Locale> localeSet = new HashSet<Locale>();
        ULocale[] icuLocales = ICUResourceBundle.getAvailableULocales();

        for (ULocale uloc : icuLocales) {
            String language = uloc.getLanguage();
            String country = uloc.getCountry();
            String variant = uloc.getVariant();
            if (language.length() >= 3 && !addIso3Languages()) {
                continue;
            }
            addLocale(new Locale(language, country, variant), localeSet);
        }

        for (Locale l : SPECIAL_LOCALES) {
            addLocale(l, localeSet);
        }

        LOCALES = localeSet.toArray(new Locale[0]);
        return LOCALES;
    }

    private static void addLocale(Locale loc, Set<Locale> locales) {
        locales.add(loc);

        if (addIcuVariants()) {
            // Add ICU variant
            String language = loc.getLanguage();
            String country = loc.getCountry();
            String variant = loc.getVariant();

            StringBuffer var = new StringBuffer(variant);
            if (var.length() != 0) {
                var.append("_");
            }
            var.append(getIcuSuffix());
            locales.add(new Locale(language, country, var.toString()));
        }
    }

    private static boolean addIso3Languages() {
        initConfig();
        return ADD_ISO3_LANG.booleanValue();
    }

    private static boolean addIcuVariants() {
        initConfig();
        return ADD_VARIANTS.booleanValue();
    }

    private static String getIcuSuffix() {
        initConfig();
        return SUFFIX;
    }

    private static synchronized void initConfig() {
        if (SUFFIX != null) {
            return;
        }

        Properties spiConfigProps = new Properties();
        try {
            InputStream is = ClassLoader.getSystemResourceAsStream(SPI_PROP_FILE);
            spiConfigProps.load(is);

            String addVariants = (String)spiConfigProps.get(ADD_VARIANTS_KEY);
            ADD_VARIANTS = Boolean.parseBoolean(addVariants);

            String addIso3Lang = (String)spiConfigProps.get(ADD_ISO3_LANG_KEY);
            ADD_ISO3_LANG = Boolean.parseBoolean(addIso3Lang);

            SUFFIX = (String)spiConfigProps.get(SUFFIX_KEY);
        } catch (IOException ioe) {
            // Any IO errors, ignore
        }
        if (ADD_ISO3_LANG == null) {
            ADD_ISO3_LANG = Boolean.valueOf(DEFAULT_ADD_VARIANTS);
        }
        if (ADD_VARIANTS == null) {
            ADD_VARIANTS = Boolean.valueOf(DEFAULT_ADD_ISO3_LANG);
        }
        if (SUFFIX == null) {
            SUFFIX = DEFAULT_SUFFIX;
        }
    }
}
