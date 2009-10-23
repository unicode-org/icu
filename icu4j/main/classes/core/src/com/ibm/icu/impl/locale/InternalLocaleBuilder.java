/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.ibm.icu.impl.locale.LanguageTag.ParseStatus;

public final class InternalLocaleBuilder {

    private String _language = "";
    private String _script = "";
    private String _region = "";
    private String _variant = "";
    private SortedMap<Character, Extension> _extMap;

    private final boolean _lenientVariant;

    private static final String LOCALESEP = "_";

    public InternalLocaleBuilder() {
        this(false);
    }

    public InternalLocaleBuilder(boolean lenientVariant) {
        _lenientVariant = lenientVariant;
    }

    public boolean isLenientVariant() {
        return _lenientVariant;
    }

    public InternalLocaleBuilder setLanguage(String language) throws LocaleSyntaxException {
        String newval = "";
        if (language.length() > 0) {
            if (!LanguageTag.isLanguage(language)) {
                throw new LocaleSyntaxException("Ill-formed language: " + language, 0);
            }
            newval = LanguageTag.canonicalizeLanguage(language);
        }
        _language = newval;
        return this;
    }

    public InternalLocaleBuilder setScript(String script) throws LocaleSyntaxException {
        String newval = "";
        if (script.length() > 0) {
            if (!LanguageTag.isScript(script)) {
                throw new LocaleSyntaxException("Ill-formed script: " + script, 0);
            }
            newval = LanguageTag.canonicalizeScript(script);
        }
        _script = newval;
        return this;
    }

    public InternalLocaleBuilder setRegion(String region) throws LocaleSyntaxException {
        String newval = "";
        if (region.length() > 0) {
            if (!LanguageTag.isRegion(region)) {
                throw new LocaleSyntaxException("Ill-formed region: " + region);
            }
            newval = LanguageTag.canonicalizeRegion(region);
        }
        _region = newval;
        return this;
    }

    public InternalLocaleBuilder setVariant(String variant) throws LocaleSyntaxException {
        String newval = "";
        if (variant.length() > 0) {
            if (_lenientVariant) {
                newval = variant;
            } else {
                newval = processVariant(variant);
            }
        }
        _variant = newval;
        return this;
    }

    public InternalLocaleBuilder setUnicodeLocaleExtension(String key, String type) throws LocaleSyntaxException {
        if (key.length() == 0) {
            throw new LocaleSyntaxException("Empty Unicode locale extension key");
        }
        if (!UnicodeLocaleExtension.isKey(key)) {
            throw new LocaleSyntaxException("Ill-formed Unicode locale extension key: " + key, 0);
        }

        key = UnicodeLocaleExtension.canonicalizeKey(key);

        UnicodeLocaleExtension ulext = null;
        if (_extMap != null) {
            ulext = (UnicodeLocaleExtension)_extMap.get(Character.valueOf(UnicodeLocaleExtension.SINGLETON));
        }

        if (type.length() == 0) {
            if (ulext != null) {
                ulext.remove(key);
            }
        } else {
            StringBuilder buf = new StringBuilder();
            StringTokenIterator sti = new StringTokenIterator(type, LanguageTag.SEP);
            for (String subtag = sti.first(); !sti.isDone(); subtag = sti.next()) {
                if (!UnicodeLocaleExtension.isTypeSubtag(subtag)) {
                    throw new LocaleSyntaxException("Ill-formed Unicode locale extension type: " + type, sti.currentStart());
                }
                if (buf.length() > 0) {
                    buf.append(LanguageTag.SEP);
                }
                buf.append(UnicodeLocaleExtension.canonicalizeTypeSubtag(subtag));
            }
            if (ulext == null) {
                SortedMap<String, String> ktmap = new TreeMap<String, String>();
                ktmap.put(key, buf.toString());
                ulext = new UnicodeLocaleExtension(ktmap);
                if (_extMap == null) {
                    _extMap = new TreeMap<Character, Extension>();
                }
                _extMap.put(Character.valueOf(UnicodeLocaleExtension.SINGLETON), ulext);
            } else {
                ulext.put(key, buf.toString());
            }
        }

        return this;
    }

    public InternalLocaleBuilder setExtension(char singleton, String value) throws LocaleSyntaxException {
        String strSingleton = String.valueOf(singleton);
        if (!LanguageTag.isExtensionSingleton(strSingleton) && !LanguageTag.isPrivateuseSingleton(strSingleton)) {
            throw new LocaleSyntaxException("Ill-formed extension key: " + singleton);
        }

        strSingleton = LanguageTag.canonicalizeExtensionSingleton(strSingleton);
        Character key = Character.valueOf(strSingleton.charAt(0));

        if (value.length() == 0) {
            if (_extMap != null) {
                _extMap.remove(key);
            }
        } else {
            StringTokenIterator sti = new StringTokenIterator(value, LanguageTag.SEP);
            ParseStatus sts = new ParseStatus();

            Extension ext = Extension.create(key.charValue(), sti, sts);
            if (sts.isError()) {
                throw new LocaleSyntaxException(sts.errorMsg, sts.errorIndex);
            }
            if (sts.parseLength != value.length() || ext == null) {
                throw new LocaleSyntaxException("Ill-formed extension value: " + value, sti.currentStart());
            }
            if (_extMap == null) {
                _extMap = new TreeMap<Character, Extension>();
            }
            _extMap.put(key, ext);
        }
        return this;
    }

    public InternalLocaleBuilder setLocale(BaseLocale base, LocaleExtensions extensions) throws LocaleSyntaxException {
        String language = base.getLanguage();
        String script = base.getScript();
        String region = base.getRegion();
        String variant = base.getVariant();

        // Validate base locale fields before updating internal state.
        // LocaleExtensions always store validated/canonicalized values,
        // so no checks are necessary.
        if (language.length() > 0) {
            if (!LanguageTag.isLanguage(language)) {
                throw new LocaleSyntaxException("Ill-formed language: " + language);
            }
            language = LanguageTag.canonicalizeLanguage(language);
        }
        if (script.length() > 0) {
            if (!LanguageTag.isScript(script)) {
                throw new LocaleSyntaxException("Ill-formed script: " + script);
            }
            script = LanguageTag.canonicalizeScript(script);
        }
        if (region.length() > 0) {
            if (!LanguageTag.isRegion(region)) {
                throw new LocaleSyntaxException("Ill-formed region: " + region);
            }
            region = LanguageTag.canonicalizeRegion(region);
        }
        if (_lenientVariant) {
            // In lenient variant mode, parse special private use value
            // reserved for Java Locale.
            String privuse = extensions.getExtensionValue(Character.valueOf(LanguageTag.PRIVATEUSE.charAt(0)));
            if (privuse != null) {
                variant = LanguageTag.getJavaCompatibleVariant(variant, privuse);
            }
        } else {
            if (variant.length() > 0) {
                variant = processVariant(variant);
            }
        }

        // update builder's internal fields
        _language = language;
        _script = script;
        _region = region;
        _variant = variant;

        // empty extensions
        if (_extMap == null) {
            _extMap = new TreeMap<Character, Extension>();
        } else {
            _extMap.clear();
        }

        Set<Character> extKeys = extensions.getKeys();
        for (Character key : extKeys) {
            Extension ext = extensions.getExtension(key);
            if (_lenientVariant && (ext instanceof PrivateuseExtension)) {
                String modPrivuse = LanguageTag.getJavaCompatiblePrivateuse(ext.getValue());
                if (!modPrivuse.equals(ext.getValue())) {
                    ext = new PrivateuseExtension(modPrivuse);
                }
            }
            _extMap.put(key, ext);
        }

        return this;
    }

    public InternalLocaleBuilder clear() {
        _language = "";
        _script = "";
        _region = "";
        _variant = "";
        removeLocaleExtensions();
        return this;
    }

    public InternalLocaleBuilder removeLocaleExtensions() {
        if (_extMap != null) {
            _extMap.clear();
        }
        return this;
    }

    public BaseLocale getBaseLocale() {
        return BaseLocale.getInstance(_language, _script, _region, _variant);
    }

    public LocaleExtensions getLocaleExtensions() {
        if (_extMap != null && _extMap.size() > 0) {
            return LocaleExtensions.getInstance(_extMap);
        }
        return LocaleExtensions.EMPTY_EXTENSIONS;
    }

    private String processVariant(String variant) throws LocaleSyntaxException {
        StringTokenIterator sti = new StringTokenIterator(variant, LOCALESEP);
        ParseStatus sts = new ParseStatus();

        List<String> variants = LanguageTag.DEFAULT_PARSER.parseVariants(sti, sts);
        if (sts.parseLength != variant.length()) {
            throw new LocaleSyntaxException("Ill-formed variant: " + variant, sti.currentStart());
        }

        StringBuilder buf = new StringBuilder();
        for (String var : variants) {
            if (buf.length() != 0) {
                buf.append(LOCALESEP);
            }
            buf.append(var);
        }
        return buf.toString();
    }
}
