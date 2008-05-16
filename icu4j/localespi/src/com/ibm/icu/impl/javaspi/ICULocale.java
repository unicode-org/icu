/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import com.ibm.icu.impl.ICUResourceBundle;

public class ICULocale {
    private static final String SPI_PROP_FILE = "com/ibm/icu/impl/javaspi/ICUProvider.properties";
    private static final String SUFFIX_KEY = "com.ibm.icu.impl.javaspi.ICULocale.suffix";
    private static final String DEFAULT_SUFFIX = "ICU";

    private static final Locale[] SPECIAL_LOCALES = {
        new Locale("ja", "JP", "JP"),
    };

    private static Locale[] LOCALES = null;
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

    private static synchronized Locale[] getLocales() {
        if (LOCALES == null) {
            List<Locale> allLocales = new ArrayList<Locale>();

            Locale[] baseLocales = ICUResourceBundle.getAvailableLocales();
            for (Locale l : baseLocales) {
                addLocale(l, allLocales);
            }
            for (Locale l : SPECIAL_LOCALES) {
                addLocale(l, allLocales);
            }
            LOCALES = allLocales.toArray(new Locale[0]);
        }
        return LOCALES;
    }

    private static void addLocale(Locale loc, List<Locale> locales) {
        locales.add(loc);

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

    private static synchronized String getIcuSuffix() {
        if (SUFFIX == null) {
            Properties spiConfigProps = new Properties();
            try {
                InputStream is = ClassLoader.getSystemResourceAsStream(SPI_PROP_FILE);
                spiConfigProps.load(is);
                SUFFIX = (String)spiConfigProps.get(SUFFIX_KEY);
            } catch (IOException ioe) {
                // Any IO errors, ignore
            }
            if (SUFFIX == null) {
                SUFFIX = DEFAULT_SUFFIX;
            }
        }
        return SUFFIX;
    }
}
