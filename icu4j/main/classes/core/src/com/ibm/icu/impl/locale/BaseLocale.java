/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl.locale;



public final class BaseLocale {

    private String _language = "";
    private String _script = "";
    private String _region = "";
    private String _variant = "";

    private transient String _id = "";
    private transient String _java6string = "";
    private transient BaseLocale _parent;

    private static final char SEPCHAR = '_';

    private static final LocaleObjectCache<BaseLocaleKey,BaseLocale> BASELOCALECACHE
        = new LocaleObjectCache<BaseLocaleKey,BaseLocale>();

    public static final BaseLocale ROOT = new BaseLocale("", "", "", "");

    private BaseLocale(String language, String script, String region, String variant) {
        if (language != null) {
            _language = language;
        }
        if (script != null) {
            _script = script;
        }
        if (region != null) {
            _region = region;
        }
        if (variant != null) {
            _variant = variant;
        }
    }

    public static BaseLocale getInstance(String language, String script, String region, String variant) {
        BaseLocaleKey key = new BaseLocaleKey(language, script, region, variant);
        BaseLocale baseLocale = BASELOCALECACHE.get(key);
        if (baseLocale == null) {
            // Create a canonical BaseLocale instance
            baseLocale = new BaseLocale(language, script, region, variant).canonicalize();
            BASELOCALECACHE.put(baseLocale.createKey(), baseLocale);
        }
        return baseLocale;
    }

    public boolean equals(Object obj) {
        return (this == obj) ||
                ((obj instanceof BaseLocale) && _id == (((BaseLocale)obj)._id));
    }

    public int hashCode() {
        return _id.hashCode();
    }

    public String getJava6String() {
        return _java6string;
    }

    public String getLanguage() {
        return _language;
    }

    public String getScript() {
        return _script;
    }

    public String getRegion() {
        return _region;
    }

    public String getVariant() {
        return _variant;
    }

    public BaseLocale getParent() {
        return _parent;
    }

    public String getID() {
        return _id;
    }

    public String toString() {
        return _id;
    }

    private BaseLocale canonicalize() {

        StringBuilder id = new StringBuilder();

        int languageLen = _language.length();
        int scriptLen = _script.length();
        int regionLen = _region.length();
        int variantLen = _variant.length();

        if (languageLen > 0) {
            // language to lower case
            _language = AsciiUtil.toLowerString(_language).intern();

            id.append(_language);
        }

        if (scriptLen > 0) {
            // script - the first letter to upper case, the rest to lower case
            StringBuilder buf = new StringBuilder();
            buf.append(AsciiUtil.toUpper(_script.charAt(0)));
            for (int i = 1; i < _script.length(); i++) {
                buf.append(AsciiUtil.toLower(_script.charAt(i)));
            }
            _script = buf.toString().intern();

            id.append(SEPCHAR);
            id.append(_script);
        }

        if (regionLen > 0) {
            // region to upper case
            _region = AsciiUtil.toUpperString(_region).intern();

            id.append(SEPCHAR);
            id.append(_region);
        }

        if (variantLen > 0) {
            // variant is case sensitive in JDK
            _variant = _variant.intern();

            if (regionLen == 0) {
                id.append(SEPCHAR);
            }
            id.append(SEPCHAR);
            id.append(_variant);
        }

        _id = id.toString().intern();

        // Compose legacy JDK ID string if required
        if (languageLen == 0 && regionLen == 0 && variantLen > 0) {
            _java6string = "";
        } else if (scriptLen > 0 || (regionLen == 0 && variantLen > 0)) {
            StringBuilder buf = new StringBuilder(_language);
            if (regionLen > 0) {
                buf.append(SEPCHAR);
                buf.append(_region);
            } else if (variantLen > 0) {
                buf.append(SEPCHAR);
            }
            if (variantLen > 0) {
                buf.append(SEPCHAR);
                buf.append(_variant);
            }
            _java6string = buf.toString().intern();
        } else {
            _java6string = _id;
        }

        // Resolve parent
        if (variantLen > 0) {
            // variant field in Java Locale may contain multiple
            // subtags
            int lastSep = _variant.lastIndexOf(SEPCHAR);
            if (lastSep == -1) {
                _parent = getInstance(_language, _script, _region, "");
            } else {
                _parent = getInstance(_language, _script, _region, _variant.substring(0, lastSep));
            }
        } else if (regionLen > 0) {
            _parent = getInstance(_language, _script, "", "");
        } else if (scriptLen > 0) {
            _parent = getInstance(_language, "", "", "");
        } else if (languageLen > 0) {
            _parent = ROOT;
        } else {
            // This is the root
            // We should never get here, because ROOT is pre-populated.
            _parent = null;
        }
        return this;
    }

    private BaseLocaleKey createKey() {
        return new BaseLocaleKey(_language, _script, _region, _variant);
    }

    public static class BaseLocaleKey implements Comparable<BaseLocaleKey> {
        private String _lang = "";
        private String _scrt = "";
        private String _regn = "";
        private String _vart = "";

        private int _hash; // Default to 0

        public BaseLocaleKey(String language, String script, String region, String variant) {
            if (language != null) {
                _lang = language;
            }
            if (script != null) {
                _scrt = script;
            }
            if (region != null) {
                _regn = region;
            }
            if (variant != null) {
                _vart = variant;
            }
        }

        public boolean equals(Object obj) {
            return (this == obj) ||
                    (obj instanceof BaseLocaleKey)
                    && AsciiUtil.caseIgnoreMatch(((BaseLocaleKey)obj)._lang, this._lang)
                    && AsciiUtil.caseIgnoreMatch(((BaseLocaleKey)obj)._scrt, this._scrt)
                    && AsciiUtil.caseIgnoreMatch(((BaseLocaleKey)obj)._regn, this._regn)
                    && ((BaseLocaleKey)obj)._vart.equals(_vart); // variant is case sensitive in JDK!
        }

        public int compareTo(BaseLocaleKey other) {
            int res = AsciiUtil.caseIgnoreCompare(this._lang, other._lang);
            if (res == 0) {
                res = AsciiUtil.caseIgnoreCompare(this._scrt, other._scrt);
                if (res == 0) {
                    res = AsciiUtil.caseIgnoreCompare(this._regn, other._regn);
                    if (res == 0) {
                        res = AsciiUtil.caseIgnoreCompare(this._vart, other._vart);
                    }
                }
            }
            return res;
        }

        public int hashCode() {
            int h = _hash;
            if (h == 0) {
                // Generating a hash value from language, script, region and variant
                for (int i = 0; i < _lang.length(); i++) {
                    h = 31*h + AsciiUtil.toLower(_lang.charAt(i));
                }
                for (int i = 0; i < _scrt.length(); i++) {
                    h = 31*h + AsciiUtil.toLower(_scrt.charAt(i));
                }
                for (int i = 0; i < _regn.length(); i++) {
                    h = 31*h + AsciiUtil.toLower(_regn.charAt(i));
                }
                for (int i = 0; i < _vart.length(); i++) {
                    h = 31*h + AsciiUtil.toLower(_vart.charAt(i));
                }
                _hash = h;
            }
            return h;
        }
    }
}
