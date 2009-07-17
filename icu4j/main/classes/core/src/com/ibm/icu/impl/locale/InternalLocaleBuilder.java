/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class InternalLocaleBuilder {

    public static final char PRIVATEUSEKEY = 'x';

    private String _language = "";
    private String _script = "";
    private String _region = "";
    private String _variant = "";

    private FieldHandler _handler = FieldHandler.DEFAULT;

    private HashMap<Character, String> _extMap;
    private HashMap<String, String> _kwdMap;

    private static final char LDMLSINGLETON = 'u';

    private static final String LANGTAGSEP = "-";
    private static final String LOCALESEP = "_";

    private static final int DEFAULTMAPCAPACITY = 4;

    public InternalLocaleBuilder() {
    }

    public InternalLocaleBuilder(FieldHandler handler) {
        _handler = handler;
    }

    public InternalLocaleBuilder setLanguage(String language) throws LocaleSyntaxException {
        String newval = "";
        if (language.length() > 0) {
            newval = _handler.process(FieldType.LANGUAGE, language);
            if (newval == null) {
                throw new LocaleSyntaxException("Ill-formed language: " + language);
            }
        }
        _language = newval;
        return this;
    }

    public InternalLocaleBuilder setScript(String script) throws LocaleSyntaxException {
        String newval = "";
        if (script.length() > 0) {
            newval = _handler.process(FieldType.SCRIPT, script);
            if (newval == null) {
                throw new LocaleSyntaxException("Ill-formed script: " + script);
            }
        }
        _script = newval;
        return this;
    }

    public InternalLocaleBuilder setRegion(String region) throws LocaleSyntaxException {
        String newval = "";
        if (region.length() > 0) {
            newval = _handler.process(FieldType.REGION, region);
            if (newval == null) {
                throw new LocaleSyntaxException("Ill-formed region: " + region);
            }
        }
        _region = newval;
        return this;
    }

    public InternalLocaleBuilder setVariant(String variant) throws LocaleSyntaxException {
        String newval = "";
        if (variant.length() > 0) {
            newval = _handler.process(FieldType.VARIANT, variant);
            if (newval == null) {
                throw new LocaleSyntaxException("Ill-formed variant: " + variant);
            }
        }
        _variant = newval;
        return this;
    }

    public InternalLocaleBuilder setLDMLExtensionValue(String key, String type) throws LocaleSyntaxException {
        if (key.length() == 0) {
            throw new LocaleSyntaxException("Empty LDML extension key");
        }
        String kwdkey = _handler.process(FieldType.LDMLKEY, key);
        if (kwdkey == null) {
            throw new LocaleSyntaxException("Ill-formed LDML extension key: " + key);
        }

        if (type.length() == 0) {
            if (_kwdMap != null) {
                _kwdMap.remove(kwdkey);
            }
        } else {
            String kwdtype = _handler.process(FieldType.LDMLTYPE, type);
            if (kwdtype == null) {
                throw new LocaleSyntaxException("Ill-formed LDML extension value: " + type);
            }
            if (_kwdMap == null) {
                _kwdMap = new HashMap<String, String>(DEFAULTMAPCAPACITY);
            }
            _kwdMap.put(kwdkey, kwdtype);
        }

        return this;
    }

    public InternalLocaleBuilder setExtension(char singleton, String value) throws LocaleSyntaxException {
        if (!LocaleExtensions.isValidExtensionKey(singleton)) {
            throw new LocaleSyntaxException("Ill-formed extension key: " + singleton);
        }

        // singleton char to lower case
        singleton = AsciiUtil.toLower(singleton);

        if (singleton == LDMLSINGLETON) {
            // 'u' extension reserved for locale keywords
            if (_kwdMap != null) {
                // blow out the keywords currently set
                _kwdMap.clear();
            }
            // parse locale keyword extension subtags
            String[] kwdtags = (value.replaceAll(LOCALESEP, LANGTAGSEP)).split(LANGTAGSEP);
            if ((kwdtags.length % 2) != 0) {
                // number of keyword subtags must be even number
                throw new LocaleSyntaxException("Ill-formed LDML extension key/value pairs: " + value);
            }
            int idx = 0;
            while (idx < kwdtags.length) {
                String kwdkey = _handler.process(FieldType.LDMLKEY, kwdtags[idx++]);
                String kwdtype = _handler.process(FieldType.LDMLTYPE, kwdtags[idx++]);
                if (kwdkey == null || kwdkey.length() == 0
                        || kwdtype == null || kwdtype.length() == 0) {
                    throw new LocaleSyntaxException("Ill-formed LDML extension key/value pairs: " + value);
                }
                if (_kwdMap == null) {
                    _kwdMap = new HashMap<String, String>(kwdtags.length / 2);
                }
                String prevVal = _kwdMap.put(kwdkey, kwdtype);
                if (prevVal != null) {
                    throw new LocaleSyntaxException("Ill-formed LDML extension containing duplicated keys: " + value);
                }
            }
        } else {
            // other extensions including privateuse
            if (value.length() == 0) {
                if (_extMap != null) {
                    _extMap.remove(Character.valueOf(singleton));
                }
            } else {
                FieldType ftype = (singleton == PRIVATEUSEKEY) ? FieldType.PRIVATEUSE : FieldType.EXTENSION;
                String extval = _handler.process(ftype, value);
                if (extval == null) {
                    throw new LocaleSyntaxException("Ill-formed LDML extension value: " + value);
                }
                if (_extMap == null) {
                    _extMap = new HashMap<Character, String>(DEFAULTMAPCAPACITY);
                }
                _extMap.put(Character.valueOf(singleton), extval);
            }
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
        if (_kwdMap != null) {
            _kwdMap.clear();
        }
        return this;
    }

    public BaseLocale getBaseLocale() {
        return BaseLocale.getInstance(_language, _script, _region, _variant);
    }

    public LocaleExtensions getLocaleExtensions() {
        TreeMap<Character, String> extMap = null;
        TreeMap<String, String> kwdMap = null;

        // process keywords
        if (_kwdMap != null && _kwdMap.size() > 0) {
            Set<Map.Entry<String, String>> kwds = _kwdMap.entrySet();
            for (Map.Entry<String, String> kwd : kwds) {
                String key = kwd.getKey();
                String type = kwd.getValue();
                if (kwdMap == null) {
                    kwdMap = new TreeMap<String, String>();
                }
                kwdMap.put(key.intern(), type.intern());
            }
        }

        // process extensions and privateuse
        if (_extMap != null) {
            Set<Map.Entry<Character, String>> exts = _extMap.entrySet();
            for (Map.Entry<Character, String> ext : exts) {
                Character key = ext.getKey();
                String value = ext.getValue();
                if (extMap == null) {
                    extMap = new TreeMap<Character, String>();
                }
                extMap.put(key, value.intern());
            }
        }

        // set canonical locale keyword extension string to the extension map
        if (kwdMap != null) {
            StringBuilder buf = new StringBuilder();
            LocaleExtensions.keywordsToString(kwdMap, buf);
            if (extMap == null) {
                extMap = new TreeMap<Character, String>();
            }
            extMap.put(Character.valueOf(LDMLSINGLETON), buf.toString().intern());
        }

        return LocaleExtensions.getInstance(extMap, kwdMap);
    }

    protected enum FieldType {
        LANGUAGE,
        SCRIPT,
        REGION,
        VARIANT,
        LDMLKEY,
        LDMLTYPE,
        EXTENSION,
        PRIVATEUSE
    }

    public static class FieldHandler {
        public static FieldHandler DEFAULT = new FieldHandler();

        protected FieldHandler() {
        }

        public String process(FieldType type, String value) {
            value = map(type, value);
            if (value.length() > 0 && !validate(type, value)) {
                return null;
            }
            return value;
        }

        protected String map(FieldType type, String value) {
            switch (type) {
            case LANGUAGE:
                value = AsciiUtil.toLowerString(value);
                break;
            case SCRIPT:
                if (value.length() > 0) {
                    StringBuilder buf = new StringBuilder();
                    buf.append(AsciiUtil.toUpper(value.charAt(0)));
                    for (int i = 1; i < value.length(); i++) {
                        buf.append(AsciiUtil.toLower(value.charAt(i)));
                    }
                    value = buf.toString();
                }
                break;
            case REGION:
                value = AsciiUtil.toUpperString(value);
                break;
            case VARIANT:
                // Java variant is case sensitive - so no case mapping here
                value = value.replaceAll(LANGTAGSEP, LOCALESEP);
                break;
            case LDMLKEY:
            case LDMLTYPE:
            case EXTENSION:
            case PRIVATEUSE:
                value = AsciiUtil.toLowerString(value).replaceAll(LOCALESEP, LANGTAGSEP);
                break;
            }
            return value;
        }

        protected boolean validate(FieldType type, String value) {
            boolean isValid = false;
            String[] subtags;

            switch (type) {
            case LANGUAGE:
                isValid = LanguageTag.isLanguageSubtag(value);
                break;
            case SCRIPT:
                isValid = LanguageTag.isScriptSubtag(value);
                break;
            case REGION:
                isValid = LanguageTag.isRegionSubtag(value);
                break;
            case VARIANT:
                // variant field could have multiple subtags
                subtags = value.split(LOCALESEP);
                for (String subtag : subtags) {
                    isValid = LanguageTag.isVariantSubtag(subtag);
                    if (!isValid) {
                        break;
                    }
                }
                break;
            case LDMLKEY:
                isValid = LocaleExtensions.isValidLDMLKey(value);
                break;
            case LDMLTYPE:
                isValid = LocaleExtensions.isValidLDMLType(value);
                break;
            case EXTENSION:
                subtags = value.split(LANGTAGSEP);
                for (String subtag : subtags) {
                    isValid = LanguageTag.isExtensionSubtag(subtag);
                    if (!isValid) {
                        break;
                    }
                }
                break;
            case PRIVATEUSE:
                subtags = value.split(LANGTAGSEP);
                for (String subtag : subtags) {
                    isValid = LanguageTag.isPrivateuseValueSubtag(subtag);
                    if (!isValid) {
                        break;
                    }
                }
                break;
            }
            return isValid;
        }
    }
}
