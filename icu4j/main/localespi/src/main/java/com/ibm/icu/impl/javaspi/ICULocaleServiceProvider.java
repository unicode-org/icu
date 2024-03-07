// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Builder;

public class ICULocaleServiceProvider {
    private static final String SPI_PROP_FILE = "com/ibm/icu/impl/javaspi/ICULocaleServiceProviderConfig.properties";

    private static final String SUFFIX_KEY = "com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.icuVariantSuffix";
    private static final String ENABLE_VARIANTS_KEY = "com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.enableIcuVariants";
    private static final String ENABLE_ISO3_LANG_KEY = "com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.enableIso3Languages";
    private static final String USE_DECIMALFORMAT_KEY = "com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.useDecimalFormat";

    private static boolean configLoaded = false;

    private static String suffix = "ICU4J";
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

    private static Map<Locale, Locale> SPECIAL_LOCALES_MAP = null;

    private static Locale[] LOCALES = null;

    public static Locale[] getAvailableLocales() {
        Locale[] all = getLocales();
        return Arrays.copyOf(all, all.length);
    }

    public static ULocale toULocaleNoSpecialVariant(Locale locale) {
        // If the given Locale has legacy ill-formed variant
        // reserved by JDK, use the map to resolve the locale.
        Locale spLoc = getSpecialLocalesMap().get(locale);
        if (spLoc != null) {
            return ULocale.forLocale(spLoc);
        }

        // The locale may have script field.
        // So we once convert it to ULocale, then strip the ICU suffix off
        // if necessary.
        ULocale result = ULocale.forLocale(locale);
        String variant = result.getVariant();
        String suffix = getIcuSuffix();
        String variantNoSuffix = null;
        if (variant.equals(suffix)) {
            variantNoSuffix = "";
        } else if (variant.endsWith(suffix) && variant.charAt(variant.length() - suffix.length() - 1) == '_') {
            variantNoSuffix = variant.substring(0, variant.length() - suffix.length() - 1);
        }
        if (variantNoSuffix == null) {
            return result;
        }

        // Strip off ICU's special suffix - cannot use Builder because
        // original locale may have ill-formed variant
        StringBuilder id = new StringBuilder(result.getLanguage());
        String script = result.getScript();
        String country = result.getCountry();
        if (script.length() > 0) {
            id.append('_');
            id.append(script);
        }
        if (country.length() > 0 || variantNoSuffix.length() > 0) {
            id.append('_');
            id.append(country);
        }
        if (variantNoSuffix.length() > 0) {
            id.append('_');
            id.append(variantNoSuffix);
        }
        String orgID = result.getName();
        int kwdIdx = orgID.indexOf('@');
        if (kwdIdx >= 0) {
            id.append(orgID.substring(kwdIdx));
        }
        return new ULocale(id.toString());
    }

    public static boolean useDecimalFormat() {
        loadConfiguration();
        return useDecimalFormat;
    }

    private static synchronized Map<Locale, Locale> getSpecialLocalesMap() {
        if (SPECIAL_LOCALES_MAP != null) {
            return SPECIAL_LOCALES_MAP;
        }

        Map<Locale, Locale> splocs = new HashMap<>();
        for (Locale spLoc : SPECIAL_LOCALES) {
            String var = spLoc.getVariant();
            if (var.length() > 0) {
                splocs.put(new Locale(spLoc.getLanguage(), spLoc.getCountry(), var + "_" + getIcuSuffix()), spLoc);
            }
        }
        SPECIAL_LOCALES_MAP = Collections.unmodifiableMap(splocs);
        return SPECIAL_LOCALES_MAP;
    }

    private static synchronized Locale[] getLocales() {
        if (LOCALES != null) {
            return LOCALES;
        }

        Set<Locale> localeSet = new HashSet<>();
        ULocale[] icuLocales = ICUResourceBundle.getAvailableULocales();

        for (ULocale uloc : icuLocales) {
            String language = uloc.getLanguage();
            if (language.length() >= 3 && !enableIso3Languages()) {
                continue;
            }
            addULocale(uloc, localeSet);

            if (uloc.getScript().length() > 0 && uloc.getCountry().length() > 0) {
                // ICU's available locales do not contain language+country
                // locales if script is available. Need to add them too.
                Builder locBld = new Builder();
                try {
                    locBld.setLocale(uloc);
                    locBld.setScript(null);
                    ULocale ulocWithoutScript = locBld.build();
                    addULocale(ulocWithoutScript, localeSet);
                } catch (Exception e) {
                    // ignore
                }
            }
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

            StringBuilder var = new StringBuilder(variant);
            if (var.length() != 0) {
                var.append("_");
            }
            var.append(getIcuSuffix());
            locales.add(new Locale(language, country, var.toString()));
        }
    }

    private static void addULocale(ULocale uloc, Set<Locale> locales) {
        locales.add(uloc.toLocale());

        if (enableIcuVariants()) {
            // Add ICU variant
            StringBuilder var = new StringBuilder(uloc.getVariant());
            if (var.length() != 0) {
                var.append("_");
            }
            var.append(getIcuSuffix());

            Builder locBld = new Builder();
            try {
                locBld.setLocale(uloc);
                locBld.setVariant(var.toString());
                ULocale ulocWithVar = locBld.build();
                locales.add(ulocWithVar.toLocale());
            } catch (Exception ignored) {
                // ignore
            }
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
            try {
                spiConfigProps.load(is);
            } finally {
                is.close();
            }

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
