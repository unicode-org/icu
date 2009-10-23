/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;


public class LocaleExtensions {
    private SortedMap<Character, Extension> _map = EMPTY_MAP;
    private String _id = "";

    private static final SortedMap<Character, Extension> EMPTY_MAP =
        Collections.unmodifiableSortedMap(new TreeMap<Character, Extension>());

    private static final LocaleObjectCache<String, LocaleExtensions> LOCALEEXTENSIONS_CACHE =
        new LocaleObjectCache<String, LocaleExtensions>();


    public static LocaleExtensions EMPTY_EXTENSIONS = new LocaleExtensions();

    private LocaleExtensions() {
    }

    static LocaleExtensions getInstance(SortedMap<Character, Extension> map) {
        if (map == null || map.isEmpty()) {
            return EMPTY_EXTENSIONS;
        }
        String id = getID(map);
        LocaleExtensions exts = LOCALEEXTENSIONS_CACHE.get(id);
        if (exts == null) {
            exts = new LocaleExtensions();
            exts._map = new TreeMap<Character, Extension>(map);
            exts._id = id;
        }
        return exts;
    }

    private static String getID(SortedMap<Character, Extension> map) {
        StringBuilder buf = new StringBuilder();
        Extension privuse = null;
        if (map != null && !map.isEmpty()) {
            Set<Entry<Character, Extension>> entries = map.entrySet();
            for (Entry<Character, Extension> entry : entries) {
                Character key = entry.getKey();
                if (key.charValue() == LanguageTag.PRIVATEUSE.charAt(0)) {
                    privuse = entry.getValue();
                    continue;
                }
                if (buf.length() > 0) {
                    buf.append(LanguageTag.SEP);
                }
                buf.append(entry.getKey());
                buf.append(LanguageTag.SEP);
                buf.append(entry.getValue().getValue());
            }
        }
        if (privuse != null) {
            if (buf.length() > 0) {
                buf.append(LanguageTag.SEP);
            }
            buf.append(LanguageTag.PRIVATEUSE);
            buf.append(LanguageTag.SEP);
            buf.append(privuse.getValue());
        }
        return buf.toString();
    }

    public Set<Character> getKeys() {
        return Collections.unmodifiableSet(_map.keySet());
    }

    public Extension getExtension(Character key) {
        return _map.get(key);
    }

    public String getExtensionValue(Character key) {
        Extension ext = _map.get(key);
        if (ext == null) {
            return "";
        }
        return ext.getValue();
    }

    public Set<String> getUnicodeLocaleKeys() {
        Extension ext = _map.get(Character.valueOf(UnicodeLocaleExtension.SINGLETON));
        if (ext == null) {
            return Collections.emptySet();
        }
        assert (ext instanceof UnicodeLocaleExtension);
        return ((UnicodeLocaleExtension)ext).getKeys();
    }

    public String getUnicodeLocaleType(String unicodeLocaleKey) {
        Extension ext = _map.get(Character.valueOf(UnicodeLocaleExtension.SINGLETON));
        if (ext == null) {
            return "";
        }
        assert (ext instanceof UnicodeLocaleExtension);
        return ((UnicodeLocaleExtension)ext).getType(unicodeLocaleKey);
    }

    public String toString() {
        return _id;
    }

    public static boolean isValidKey(String key) {
        return LanguageTag.isExtensionSingleton(key) || LanguageTag.isPrivateuseSingleton(key);
    }
}
