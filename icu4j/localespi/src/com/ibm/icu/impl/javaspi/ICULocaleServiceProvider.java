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

public class ICULocaleServiceProvider {
    private static final String SPI_PROP_FILE = "com/ibm/icu/impl/javaspi/ICULocaleServiceProviderConfig.properties";

    private static final String SUFFIX_KEY = "com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.icuVariantSuffix";
    private static final String ENABLE_VARIANTS_KEY = "com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.enableIcuVariants";
    private static final String ENABLE_ISO3_LANG_KEY = "com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.enableIso3Languages";
    private static final String USE_DECIMALFORMAT_KEY = "com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.useDecimalFormat";

    private static boolean configLoaded = false;

    private static String suffix = "ICU";
    private static boolean enableVariants = true;
    private static boolean enableIso3Lang = true;
    private static boolean useDecimalFormat = false;

    private static final Locale[] SPECIAL_LOCALES = {
        new Locale("ja", "JP", "JP"),
        new Locale("no"),
        new Locale("no", "NO"),
        new Locale("no", "NO", "NY"),
        new Locale("sr", "CS"),
        new Locale("th", "TH", "TH"),
    };

    private static Locale[] LOCALES = null;

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

    public static boolean useDecimalFormat() {
        loadConfiguration();
        return useDecimalFormat;
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
            if (language.length() >= 3 && !enableIso3Languages()) {
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

        if (enableIcuVariants()) {
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

    private static boolean enableIso3Languages() {
        return enableIso3Lang;
    }

    private static boolean enableIcuVariants() {
        loadConfiguration();
        return enableVariants;
    }

    private static String getIcuSuffix() {
        loadConfiguration();
        return suffix;
    }

    private static synchronized void loadConfiguration() {
        if (configLoaded) {
            return;
        }
        Properties spiConfigProps = new Properties();
        try {
            InputStream is = ClassLoader.getSystemResourceAsStream(SPI_PROP_FILE);
            spiConfigProps.load(is);

            String val = (String)spiConfigProps.get(SUFFIX_KEY);
            if (val != null && val.length() > 0) {
                suffix = val;
            }
            enableVariants = parseBooleanString((String)spiConfigProps.get(ENABLE_VARIANTS_KEY), enableVariants);
            enableIso3Lang = parseBooleanString((String)spiConfigProps.get(ENABLE_ISO3_LANG_KEY), enableIso3Lang);
            useDecimalFormat = parseBooleanString((String)spiConfigProps.get(USE_DECIMALFORMAT_KEY), useDecimalFormat);
        } catch (IOException ioe) {
            // Any IO errors, ignore
        }
        configLoaded = true;
    }

    private static boolean parseBooleanString(String str, boolean defaultVal) {
        if (str == null) {
            return defaultVal;
        }
        if (str.equalsIgnoreCase("true")) {
            return true;
        } else if (str.equalsIgnoreCase("false")) {
            return false;
        }
        return defaultVal;
    }
}
