/*
 *******************************************************************************
 * Copyright (C) 2009-2015, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.impl.CurrencyData.CurrencyDisplayInfo;
import com.ibm.icu.impl.locale.AsciiUtil;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DisplayContext;
import com.ibm.icu.text.DisplayContext.Type;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;

public class LocaleDisplayNamesImpl extends LocaleDisplayNames {
    private final ULocale locale;
    private final DialectHandling dialectHandling;
    private final DisplayContext capitalization;
    private final DisplayContext nameLength;
    private final DataTable langData;
    private final DataTable regionData;
    private final MessageFormat separatorFormat;
    private final MessageFormat format;
    private final MessageFormat keyTypeFormat;
    private final char formatOpenParen;
    private final char formatReplaceOpenParen;
    private final char formatCloseParen;
    private final char formatReplaceCloseParen;
    private final CurrencyDisplayInfo currencyDisplayInfo;

    private static final Cache cache = new Cache();

    /**
     * Capitalization context usage types for locale display names
     */
    private enum CapitalizationContextUsage {
        LANGUAGE, 
        SCRIPT, 
        TERRITORY,
        VARIANT, 
        KEY, 
        KEYVALUE 
    }
    /**
     * Capitalization transforms. For each usage type, indicates whether to titlecase for
     * the context specified in capitalization (which we know at construction time).
     */
    private boolean[] capitalizationUsage = null;
    /**
     * Map from resource key to CapitalizationContextUsage value
     */
    private static final Map<String, CapitalizationContextUsage> contextUsageTypeMap;
    static {
        contextUsageTypeMap=new HashMap<String, CapitalizationContextUsage>();
        contextUsageTypeMap.put("languages", CapitalizationContextUsage.LANGUAGE);
        contextUsageTypeMap.put("script",    CapitalizationContextUsage.SCRIPT);
        contextUsageTypeMap.put("territory", CapitalizationContextUsage.TERRITORY);
        contextUsageTypeMap.put("variant",   CapitalizationContextUsage.VARIANT);
        contextUsageTypeMap.put("key",       CapitalizationContextUsage.KEY);
        contextUsageTypeMap.put("keyValue",  CapitalizationContextUsage.KEYVALUE);
    }
    /**
     * BreakIterator to use for capitalization
     */
    private transient BreakIterator capitalizationBrkIter = null;


    public static LocaleDisplayNames getInstance(ULocale locale, DialectHandling dialectHandling) {
        synchronized (cache) {
            return cache.get(locale, dialectHandling);
        }
    }

    public static LocaleDisplayNames getInstance(ULocale locale, DisplayContext... contexts) {
        synchronized (cache) {
            return cache.get(locale, contexts);
        }
    }

    public LocaleDisplayNamesImpl(ULocale locale, DialectHandling dialectHandling) {
        this(locale, (dialectHandling==DialectHandling.STANDARD_NAMES)? DisplayContext.STANDARD_NAMES: DisplayContext.DIALECT_NAMES,
                DisplayContext.CAPITALIZATION_NONE);
    }

    public LocaleDisplayNamesImpl(ULocale locale, DisplayContext... contexts) {
        DialectHandling dialectHandling = DialectHandling.STANDARD_NAMES;
        DisplayContext capitalization = DisplayContext.CAPITALIZATION_NONE;
        DisplayContext nameLength = DisplayContext.LENGTH_FULL;
        for (DisplayContext contextItem : contexts) {
            switch (contextItem.type()) {
            case DIALECT_HANDLING:
                dialectHandling = (contextItem.value()==DisplayContext.STANDARD_NAMES.value())?
                        DialectHandling.STANDARD_NAMES: DialectHandling.DIALECT_NAMES;
                break;
            case CAPITALIZATION:
                capitalization = contextItem;
                break;
            case DISPLAY_LENGTH:
                nameLength = contextItem;
                break;
            default:
                break;
            }
        }

        this.dialectHandling = dialectHandling;
        this.capitalization = capitalization;
        this.nameLength = nameLength;
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
            sep = "{0}, {1}";
        }
        this.separatorFormat = new MessageFormat(sep);

        String pattern = langData.get("localeDisplayPattern", "pattern");
        if ("pattern".equals(pattern)) {
            pattern = "{0} ({1})";
        }
        this.format = new MessageFormat(pattern);
        if (pattern.contains("（")) {
            formatOpenParen = '（';
            formatCloseParen = '）';
            formatReplaceOpenParen = '［';
            formatReplaceCloseParen = '］';
        } else  {
            formatOpenParen = '(';
            formatCloseParen = ')';
            formatReplaceOpenParen = '[';
            formatReplaceCloseParen = ']';
        }

        String keyTypePattern = langData.get("localeDisplayPattern", "keyTypePattern");
        if ("keyTypePattern".equals(keyTypePattern)) {
            keyTypePattern = "{0}={1}";
        }
        this.keyTypeFormat = new MessageFormat(keyTypePattern);

        // Get values from the contextTransforms data if we need them
        // Also check whether we will need a break iterator (depends on the data)
        boolean needBrkIter = false;
        if (capitalization == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU ||
                capitalization == DisplayContext.CAPITALIZATION_FOR_STANDALONE) {
            capitalizationUsage = new boolean[CapitalizationContextUsage.values().length]; // initialized to all false
            ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
            UResourceBundle contextTransformsBundle = null;
            try {
                contextTransformsBundle = (UResourceBundle)rb.getWithFallback("contextTransforms");
            }
            catch (MissingResourceException e) {
                contextTransformsBundle = null; // probably redundant
            }
            if (contextTransformsBundle != null) {
                UResourceBundleIterator ctIterator = contextTransformsBundle.getIterator();
                while ( ctIterator.hasNext() ) {
                    UResourceBundle contextTransformUsage = ctIterator.next();
                    int[] intVector = contextTransformUsage.getIntVector();
                    if (intVector.length >= 2) {
                        String usageKey = contextTransformUsage.getKey();
                        CapitalizationContextUsage usage = contextUsageTypeMap.get(usageKey);
                        if (usage != null) {
                            int titlecaseInt = (capitalization == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU)?
                                    intVector[0]: intVector[1];
                                    if (titlecaseInt != 0) {
                                        capitalizationUsage[usage.ordinal()] = true;
                                        needBrkIter = true;
                                    }
                        }
                    }
                }
            }
        }
        // Get a sentence break iterator if we will need it
        if (needBrkIter || capitalization == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE) {
            capitalizationBrkIter = BreakIterator.getSentenceInstance(locale);
        }

        this.currencyDisplayInfo = CurrencyData.provider.getInstance(locale, false);
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
    public DisplayContext getContext(DisplayContext.Type type) {
        DisplayContext result;
        switch (type) {
        case DIALECT_HANDLING:
            result = (dialectHandling==DialectHandling.STANDARD_NAMES)? DisplayContext.STANDARD_NAMES: DisplayContext.DIALECT_NAMES;
            break;
        case CAPITALIZATION:
            result = capitalization;
            break;
        case DISPLAY_LENGTH:
            result = nameLength;
            break;
        default:
            result = DisplayContext.STANDARD_NAMES; // hmm, we should do something else here
            break;
        }
        return result;
    }

    private String adjustForUsageAndContext(CapitalizationContextUsage usage, String name) {
        if (name != null && name.length() > 0 && UCharacter.isLowerCase(name.codePointAt(0)) &&
                (capitalization==DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE ||
                (capitalizationUsage != null && capitalizationUsage[usage.ordinal()]) )) {
            // Note, won't have capitalizationUsage != null && capitalizationUsage[usage.ordinal()]
            // unless capitalization is CAPITALIZATION_FOR_UI_LIST_OR_MENU or CAPITALIZATION_FOR_STANDALONE
            synchronized (this) {
                if (capitalizationBrkIter == null) {
                    // should only happen when deserializing, etc.
                    capitalizationBrkIter = BreakIterator.getSentenceInstance(locale);
                }
                return UCharacter.toTitleCase(locale, name, capitalizationBrkIter,
                        UCharacter.TITLECASE_NO_LOWERCASE | UCharacter.TITLECASE_NO_BREAK_ADJUSTMENT);
            }
        }
        return name;
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

    // TOTO: implement use of capitalization
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
            resultName = localeIdName(lang)
                    .replace(formatOpenParen, formatReplaceOpenParen)
                    .replace(formatCloseParen, formatReplaceCloseParen);
        }

        StringBuilder buf = new StringBuilder();
        if (hasScript) {
            // first element, don't need appendWithSep
            buf.append(scriptDisplayNameInContext(script)
                    .replace(formatOpenParen, formatReplaceOpenParen)
                    .replace(formatCloseParen, formatReplaceCloseParen));
        }
        if (hasCountry) {
            appendWithSep(regionDisplayName(country)
                    .replace(formatOpenParen, formatReplaceOpenParen)
                    .replace(formatCloseParen, formatReplaceCloseParen), buf);
        }
        if (hasVariant) {
            appendWithSep(variantDisplayName(variant)
                    .replace(formatOpenParen, formatReplaceOpenParen)
                    .replace(formatCloseParen, formatReplaceCloseParen), buf);
        }

        Iterator<String> keys = locale.getKeywords();
        if (keys != null) {
            while (keys.hasNext()) {
                String key = keys.next();
                String value = locale.getKeywordValue(key);
                String keyDisplayName = keyDisplayName(key)
                        .replace(formatOpenParen, formatReplaceOpenParen)
                        .replace(formatCloseParen, formatReplaceCloseParen);
                String valueDisplayName = keyValueDisplayName(key, value)
                        .replace(formatOpenParen, formatReplaceOpenParen)
                        .replace(formatCloseParen, formatReplaceCloseParen);
                if (!valueDisplayName.equals(value)) {
                    appendWithSep(valueDisplayName, buf);
                } else if (!key.equals(keyDisplayName)) {
                    String keyValue = keyTypeFormat.format(
                            new String[] { keyDisplayName, valueDisplayName });
                    appendWithSep(keyValue, buf);
                } else {
                    appendWithSep(keyDisplayName, buf)
                    .append("=")
                    .append(valueDisplayName);
                }
            }
        }

        String resultRemainder = null;
        if (buf.length() > 0) {
            resultRemainder = buf.toString();
        }

        if (resultRemainder != null) {
            resultName =  format.format(new Object[] {resultName, resultRemainder});
        }

        return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, resultName);
    }

    private String localeIdName(String localeId) {
        if (nameLength == DisplayContext.LENGTH_SHORT) {
            String locIdName = langData.get("Languages%short", localeId);
            if (!locIdName.equals(localeId)) {
                return locIdName;
            }
        }
        return langData.get("Languages", localeId);
    }

    @Override
    public String languageDisplayName(String lang) {
        // Special case to eliminate non-languages, which pollute our data.
        if (lang.equals("root") || lang.indexOf('_') != -1) {
            return lang;
        }
        if (nameLength == DisplayContext.LENGTH_SHORT) {
            String langName = langData.get("Languages%short", lang);
            if (!langName.equals(lang)) {
                return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, langName);
            }
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, langData.get("Languages", lang));
    }

    @Override
    public String scriptDisplayName(String script) {
        String str = langData.get("Scripts%stand-alone", script);
        if (str.equals(script)) {
            if (nameLength == DisplayContext.LENGTH_SHORT) {
                str = langData.get("Scripts%short", script);
                if (!str.equals(script)) {
                    return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, str);
                }
            }
            str = langData.get("Scripts", script);
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, str);
    }

    @Override
    public String scriptDisplayNameInContext(String script) {
        if (nameLength == DisplayContext.LENGTH_SHORT) {
            String scriptName = langData.get("Scripts%short", script);
            if (!scriptName.equals(script)) {
                return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, scriptName);
            }
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, langData.get("Scripts", script));
    }

    @Override
    public String scriptDisplayName(int scriptCode) {
        return scriptDisplayName(UScript.getShortName(scriptCode));
    }

    @Override
    public String regionDisplayName(String region) {
        if (nameLength == DisplayContext.LENGTH_SHORT) {
            String regionName = regionData.get("Countries%short", region);
            if (!regionName.equals(region)) {
                return adjustForUsageAndContext(CapitalizationContextUsage.TERRITORY, regionName);
            }
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.TERRITORY, regionData.get("Countries", region));
    }

    @Override
    public String variantDisplayName(String variant) {
        // don't have a resource for short variant names
        return adjustForUsageAndContext(CapitalizationContextUsage.VARIANT, langData.get("Variants", variant));
    }

    @Override
    public String keyDisplayName(String key) {
        // don't have a resource for short key names
        return adjustForUsageAndContext(CapitalizationContextUsage.KEY, langData.get("Keys", key));
    }

    @Override
    public String keyValueDisplayName(String key, String value) {
        String keyValueName = null;

        if (key.equals("currency")) {
            keyValueName = currencyDisplayInfo.getName(AsciiUtil.toUpperString(value));
            if (keyValueName == null) {
                keyValueName = value;
            }
        } else {
            if (nameLength == DisplayContext.LENGTH_SHORT) {
                String tmp = langData.get("Types%short", key, value);
                if (!tmp.equals(value)) {
                    keyValueName = tmp;
                }
            }
            if (keyValueName == null) {
                keyValueName = langData.get("Types", key, value);
            }
        }

        return adjustForUsageAndContext(CapitalizationContextUsage.KEYVALUE, keyValueName);
    }

    @Override
    public List<UiListItem> getUiListCompareWholeItems(Set<ULocale> localeSet, Comparator<UiListItem> comparator) {
        DisplayContext capContext = getContext(Type.CAPITALIZATION);

        List<UiListItem> result = new ArrayList<UiListItem>();
        Map<ULocale,Set<ULocale>> baseToLocales = new HashMap<ULocale,Set<ULocale>>();
        ULocale.Builder builder = new ULocale.Builder();
        for (ULocale locOriginal : localeSet) {
            builder.setLocale(locOriginal); // verify well-formed. We do this here so that we consistently throw exception
            ULocale loc = ULocale.addLikelySubtags(locOriginal);
            ULocale base = new ULocale(loc.getLanguage());
            Set<ULocale> locales = baseToLocales.get(base);
            if (locales == null) {
                baseToLocales.put(base, locales = new HashSet<ULocale>());
            }
            locales.add(loc);
        }
        for (Entry<ULocale, Set<ULocale>> entry : baseToLocales.entrySet()) {
            ULocale base = entry.getKey();
            Set<ULocale> values = entry.getValue();
            if (values.size() == 1) {
                ULocale locale = values.iterator().next();
                result.add(newRow(ULocale.minimizeSubtags(locale, ULocale.Minimize.FAVOR_SCRIPT), capContext));
            } else {
                Set<String> scripts = new HashSet<String>();
                Set<String> regions = new HashSet<String>();
                // need the follow two steps to make sure that unusual scripts or regions are displayed
                ULocale maxBase = ULocale.addLikelySubtags(base);
                scripts.add(maxBase.getScript());
                regions.add(maxBase.getCountry());
                for (ULocale locale : values) {
                    scripts.add(locale.getScript());
                    regions.add(locale.getCountry());
                }
                boolean hasScripts = scripts.size() > 1;
                boolean hasRegions = regions.size() > 1;
                for (ULocale locale : values) {
                    ULocale.Builder modified = builder.setLocale(locale);
                    if (!hasScripts) {
                        modified.setScript("");
                    }
                    if (!hasRegions) {
                        modified.setRegion("");
                    }
                    result.add(newRow(modified.build(), capContext));
                }
            }
        }
        Collections.sort(result, comparator);
        return result;
    }

    private UiListItem newRow(ULocale modified, DisplayContext capContext) {
        ULocale minimized = ULocale.minimizeSubtags(modified, ULocale.Minimize.FAVOR_SCRIPT); 
        String tempName = modified.getDisplayName(locale);
        boolean titlecase = capContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU;
        String nameInDisplayLocale =  titlecase ? UCharacter.toTitleFirst(locale, tempName) : tempName;
        tempName = modified.getDisplayName(modified);
        String nameInSelf = capContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU ? UCharacter.toTitleFirst(modified, tempName) : tempName;
        return new UiListItem(minimized, modified, nameInDisplayLocale, nameInSelf);
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

    private StringBuilder appendWithSep(String s, StringBuilder b) {
        if (b.length() == 0) {
            b.append(s); 
        } else {
            String combined = separatorFormat.format(new String[] { b.toString(), s });
            b.replace(0, b.length(), combined);
        }
        return b;
    }

    private static class Cache {
        private ULocale locale;
        private DialectHandling dialectHandling;
        private DisplayContext capitalization;
        private DisplayContext nameLength;
        private LocaleDisplayNames cache;
        public LocaleDisplayNames get(ULocale locale, DialectHandling dialectHandling) {
            if (!(dialectHandling == this.dialectHandling && DisplayContext.CAPITALIZATION_NONE == this.capitalization &&
                    DisplayContext.LENGTH_FULL == this.nameLength && locale.equals(this.locale))) {
                this.locale = locale;
                this.dialectHandling = dialectHandling;
                this.capitalization = DisplayContext.CAPITALIZATION_NONE;
                this.nameLength = DisplayContext.LENGTH_FULL;
                this.cache = new LocaleDisplayNamesImpl(locale, dialectHandling);
            }
            return cache;
        }
        public LocaleDisplayNames get(ULocale locale, DisplayContext... contexts) {
            DialectHandling dialectHandlingIn = DialectHandling.STANDARD_NAMES;
            DisplayContext capitalizationIn = DisplayContext.CAPITALIZATION_NONE;
            DisplayContext nameLengthIn = DisplayContext.LENGTH_FULL;
            for (DisplayContext contextItem : contexts) {
                switch (contextItem.type()) {
                case DIALECT_HANDLING:
                    dialectHandlingIn = (contextItem.value()==DisplayContext.STANDARD_NAMES.value())?
                            DialectHandling.STANDARD_NAMES: DialectHandling.DIALECT_NAMES;
                    break;
                case CAPITALIZATION:
                    capitalizationIn = contextItem;
                    break;
                case DISPLAY_LENGTH:
                    nameLengthIn = contextItem;
                    break;
                default:
                    break;
                }
            }
            if (!(dialectHandlingIn == this.dialectHandling && capitalizationIn == this.capitalization &&
                    nameLengthIn == this.nameLength && locale.equals(this.locale))) {
                this.locale = locale;
                this.dialectHandling = dialectHandlingIn;
                this.capitalization = capitalizationIn;
                this.nameLength = nameLengthIn;
                this.cache = new LocaleDisplayNamesImpl(locale, contexts);
            }
            return cache;
        }
    }
}
