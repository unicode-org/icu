/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.Iterator;
import java.util.Locale;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

public class LocaleDisplayNamesImpl extends LocaleDisplayNames {
    private final ULocale locale;
    private final DialectHandling dialectHandling;
    private final DataTable langData;
    private final DataTable regionData;
    private final Appender appender;
    private final MessageFormat format;

    private static final Cache cache = new Cache();

    public static LocaleDisplayNames getInstance(ULocale locale, DialectHandling dialectHandling) {
        synchronized (cache) {
            return cache.get(locale, dialectHandling);
        }
    }

    public LocaleDisplayNamesImpl(ULocale locale, DialectHandling dialectHandling) {
        this.dialectHandling = dialectHandling;
        this.langData = LangDataTables.impl.get(locale);
        this.regionData = RegionDataTables.impl.get(locale);
        this.locale = ULocale.ROOT.equals(langData.getLocale()) ? regionData.getLocale() :
            langData.getLocale();

        // Note, by going through DataTable, this uses table lookup rather than straight lookup.
        // That should get us the same data, I think.  This way we don't have to explicitly
        // load the bundle again.  Using direct lookup didn't seem to make an appreciable
        // difference in performance.
        String sep = langData.get("localeDisplayPattern", "separator");
        if ("separator".equals(sep)) {
            sep = ", ";
        }
        this.appender = new Appender(sep);

        String pattern = langData.get("localeDisplayPattern", "pattern");
        if ("pattern".equals(pattern)) {
            pattern = "{0} ({1})";
        }
        this.format = new MessageFormat(pattern);
    }

    @Override
    public ULocale getLocale() {
        return locale;
    }

    @Override
    public DialectHandling getDialectHandling() {
        return dialectHandling;
    }

    @Override
    public String localeDisplayName(ULocale locale) {
        return localeDisplayNameInternal(locale);
    }

    @Override
    public String localeDisplayName(Locale locale) {
        return localeDisplayNameInternal(ULocale.forLocale(locale));
    }

    @Override
    public String localeDisplayName(String localeId) {
        return localeDisplayNameInternal(new ULocale(localeId));
    }

    private String localeDisplayNameInternal(ULocale locale) {
        // lang
        // lang (script, country, variant, keyword=value, ...)
        // script, country, variant, keyword=value, ...

        String resultName = null;

        String lang = locale.getLanguage();

        // Empty basename indicates root locale (keywords are ignored for this).
        // Our data uses 'root' to access display names for the root locale in the
        // "Languages" table.
        if (locale.getBaseName().length() == 0) {
            lang = "root";
        }
        String script = locale.getScript();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        boolean hasScript = script.length() > 0;
        boolean hasCountry = country.length() > 0;
        boolean hasVariant = variant.length() > 0;

        // always have a value for lang
        if (dialectHandling == DialectHandling.DIALECT_NAMES) {
            do { // loop construct is so we can break early out of search
                if (hasScript && hasCountry) {
                    String langScriptCountry = lang + '_' + script + '_' + country;
                    String result = localeIdName(langScriptCountry);
                    if (!result.equals(langScriptCountry)) {
                        resultName = result;
                        hasScript = false;
                        hasCountry = false;
                        break;
                    }
                }
                if (hasScript) {
                    String langScript = lang + '_' + script;
                    String result = localeIdName(langScript);
                    if (!result.equals(langScript)) {
                        resultName = result;
                        hasScript = false;
                        break;
                    }
                }
                if (hasCountry) {
                    String langCountry = lang + '_' + country;
                    String result = localeIdName(langCountry);
                    if (!result.equals(langCountry)) {
                        resultName = result;
                        hasCountry = false;
                        break;
                    }
                }
            } while (false);
        }

        if (resultName == null) {
            resultName = localeIdName(lang);
        }

        StringBuilder buf = new StringBuilder();
        if (hasScript) {
            // first element, don't need appender
            buf.append(scriptDisplayName(script));
        }
        if (hasCountry) {
            appender.append(regionDisplayName(country), buf);
        }
        if (hasVariant) {
            appender.append(variantDisplayName(variant), buf);
        }

        Iterator<String> keys = locale.getKeywords();
        if (keys != null) {
            while (keys.hasNext()) {
                String key = keys.next();
                String value = locale.getKeywordValue(key);
                appender.append(keyDisplayName(key), buf)
                    .append("=")
                    .append(keyValueDisplayName(key, value));
            }
        }

        String resultRemainder = null;
        if (buf.length() > 0) {
            resultRemainder = buf.toString();
        }

        if (resultRemainder != null) {
            return format.format(new Object[] {resultName, resultRemainder});
        }

        return resultName;
    }

    private String localeIdName(String localeId) {
        return langData.get("Languages", localeId);
    }

    @Override
    public String languageDisplayName(String lang) {
        // Special case to eliminate non-languages, which pollute our data.
        if (lang.equals("root") || lang.indexOf('_') != -1) {
            return lang;
        }
        return langData.get("Languages", lang);
    }

    @Override
    public String scriptDisplayName(String script) {
        return langData.get("Scripts", script);
    }

    @Override
    public String scriptDisplayName(int scriptCode) {
        return scriptDisplayName(UScript.getShortName(scriptCode));
    }

    @Override
    public String regionDisplayName(String region) {
        return regionData.get("Countries", region);
    }

    @Override
    public String variantDisplayName(String variant) {
        return langData.get("Variants", variant);
    }

    @Override
    public String keyDisplayName(String key) {
        return langData.get("Keys", key);
    }

    @Override
    public String keyValueDisplayName(String key, String value) {
        return langData.get("Types", key, value);
    }

    public static class DataTable {
        ULocale getLocale() {
            return ULocale.ROOT;
        }

        String get(String tableName, String code) {
            return get(tableName, null, code);
        }

        String get(String tableName, String subTableName, String code) {
            return code;
        }
    }

    static class ICUDataTable extends DataTable {
        private final ICUResourceBundle bundle;

        public ICUDataTable(String path, ULocale locale) {
            this.bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(
                    path, locale.getBaseName());
        }

        public ULocale getLocale() {
            return bundle.getULocale();
        }

        public String get(String tableName, String subTableName, String code) {
            return ICUResourceTableAccess.getTableString(bundle, tableName, subTableName,
                    code);
        }
    }

    static abstract class DataTables {
        public abstract DataTable get(ULocale locale);
        public static DataTables load(String className) {
            try {
                return (DataTables) Class.forName(className).newInstance();
            } catch (Throwable t) {
                final DataTable NO_OP = new DataTable();
                return new DataTables() {
                    public DataTable get(ULocale locale) {
                        return NO_OP;
                    }
                };
            }
        }
    }

    static abstract class ICUDataTables extends DataTables {
        private final String path;

        protected ICUDataTables(String path) {
            this.path = path;
        }

        @Override
        public DataTable get(ULocale locale) {
            return new ICUDataTable(path, locale);
        }
    }

    static class LangDataTables {
        static final DataTables impl = DataTables.load("com.ibm.icu.impl.ICULangDataTables");
    }

    static class RegionDataTables {
        static final DataTables impl = DataTables.load("com.ibm.icu.impl.ICURegionDataTables");
    }

    public static enum DataTableType {
        LANG, REGION;
    }

    public static boolean haveData(DataTableType type) {
        switch (type) {
        case LANG: return LangDataTables.impl instanceof ICUDataTables;
        case REGION: return RegionDataTables.impl instanceof ICUDataTables;
        default:
            throw new IllegalArgumentException("unknown type: " + type);
        }
    }

    static class Appender {
        private final String sep;

        Appender(String sep) {
            this.sep = sep;
        }
        StringBuilder append(String s, StringBuilder b) {
            if (b.length() > 0) {
                b.append(sep);
            }
            b.append(s);
            return b;
        }
    }

    private static class Cache {
        private ULocale locale;
        private DialectHandling dialectHandling;
        private LocaleDisplayNames cache;
        public LocaleDisplayNames get(ULocale locale, DialectHandling dialectHandling) {
            if (!(dialectHandling == this.dialectHandling && locale.equals(this.locale))) {
                this.locale = locale;
                this.dialectHandling = dialectHandling;
                this.cache = new LocaleDisplayNamesImpl(locale, dialectHandling);
            }
            return cache;
        }
    }
}
