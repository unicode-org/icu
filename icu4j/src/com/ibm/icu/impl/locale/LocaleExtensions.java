/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.impl.Utility;

public final class LocaleExtensions {
    public static final LocaleExtensions EMPTY_EXTENSIONS = new LocaleExtensions("");

    private String _extensions;
//    private TreeMap<Character, String> _extMap;
    private TreeMap _extMap;
//    private TreeMap<String, String> _kwdMap;
    private TreeMap _kwdMap;

    private static final String LOCALEEXTSEP = "-";
    private static final String LDMLSINGLETON = "u";
    private static final String PRIVUSE = "x";
    private static final int MINLEN = 3; // minimum length of string representation "x-?"


    private LocaleExtensions(String extensions) {
        _extensions = extensions == null ? "" : extensions;
    }

    public static LocaleExtensions getInstance(String extensions) {
        if (extensions == null || extensions.length() == 0) {
            return EMPTY_EXTENSIONS;
        }

//        extensions = AsciiUtil.toLowerString(extensions).replaceAll("_", LOCALEEXTSEP);
        extensions = AsciiUtil.toLowerString(extensions).replaceAll("_", LOCALEEXTSEP);

        if (extensions.length() < MINLEN) {
            // malformed extensions - too short
            return new LocaleExtensions(extensions);
        }

//        TreeMap<Character, String> extMap = null;
        TreeMap extMap = null;
//        TreeMap<String, String> kwdMap = null;
        TreeMap kwdMap = null;
        boolean bParseFailure = false;

        // parse the extension subtags
//        String[] subtags = extensions.split(LOCALEEXTSEP);
        String[] subtags = Utility.split(extensions, '-');
        String letter = null;
//        extMap = new TreeMap<Character, String>();
        extMap = new TreeMap();
//        StringBuilder buf = new StringBuilder();
        StringBuffer buf = new StringBuffer();
        boolean inLocaleKeywords = false;
        boolean inPrivateUse = false;
        String kwkey = null;

        for (int i = 0; i < subtags.length; i++) {
            if (subtags[i].length() == 0) {
                // empty subtag
                bParseFailure = true;
                break;
            }
            if (subtags[i].length() == 1 && !inPrivateUse) {
                if (letter != null) {
                    // next extension singleton
                    if (extMap.containsKey(subtags[i])) {
                        // duplicated singleton extension letter
                        bParseFailure = true;
                        break;
                    }
                    // write out the previous extension
                    if (inLocaleKeywords) {
                        if (kwkey != null) {
                            // no locale keyword key
                            bParseFailure = true;
                            break;
                        }
                        // creating a single string including locale keyword key/type pairs
                        keywordsToString(kwdMap, buf);
                        inLocaleKeywords = false;
                    }
                    if (buf.length() == 0) {
                        // empty subtag
                        bParseFailure = true;
                        break;
                    }
//                    extMap.put(Character.valueOf(letter.charAt(0)), buf.toString().intern());
                    extMap.put(new Character(letter.charAt(0)), buf.toString().intern());
                }
                // preparation for next extension
                if (subtags[i].equals(LDMLSINGLETON)) {
//                    kwdMap = new TreeMap<String, String>();
                    kwdMap = new TreeMap();
                    inLocaleKeywords = true;
                } else if (subtags[i].equals(PRIVUSE)) {
                    inPrivateUse = true;
                }
                buf.setLength(0);
                letter = subtags[i];
                continue;
            }
            if (inLocaleKeywords) {
                if (kwkey == null) {
                    kwkey = subtags[i];
                } else {
                    kwdMap.put(kwkey.intern(), subtags[i].intern());
                    kwkey = null;
                }
            } else {
                // append an extension/prvate use subtag
                if (buf.length() > 0) {
                    buf.append(LOCALEEXTSEP);
                }
                buf.append(subtags[i]);
            }
        }
        if (!bParseFailure) {
            // process the last extension
            if (inLocaleKeywords) {
                if (kwkey != null) {
                    bParseFailure = true;
                } else {
                    // creating a single string including locale keyword key/type pairs
                    keywordsToString(kwdMap, buf);
                }
            }
            if (buf.length() == 0) {
                // empty subtag at the end
                bParseFailure = true;
            } else {
//                extMap.put(Character.valueOf(letter.charAt(0)), buf.toString().intern());
                extMap.put(new Character(letter.charAt(0)), buf.toString().intern());
            }
        }

        if (bParseFailure) {
            // parsing the extension string failed.
            // do not set any partial results in the result.
            return new LocaleExtensions(extensions);
        }

        String canonical = extensionsToCanonicalString(extMap);
        LocaleExtensions le = new LocaleExtensions(canonical);
        le._extMap = extMap;
        le._kwdMap = kwdMap;

        return le;
    }

    // This method assumes extension map and locale keyword map
    // are all in canonicalized format.  This method is only used by
    // InternalLocaleBuilder.
//    static LocaleExtensions getInstance(TreeMap<Character, String> extMap, TreeMap<String ,String> kwdMap) {
    public static LocaleExtensions getInstance(TreeMap extMap, TreeMap kwdMap) {
        if (extMap == null) {
            return EMPTY_EXTENSIONS;
        }
        String canonical = extensionsToCanonicalString(extMap);
        LocaleExtensions le = new LocaleExtensions(canonical);
        le._extMap = extMap;
        le._kwdMap = kwdMap;

        return le;
    }

    public boolean equals(Object obj) {
        return (this == obj) ||
            ((obj instanceof LocaleExtensions) && _extensions == (((LocaleExtensions)obj)._extensions));
    }

    public int hashCode() {
        return _extensions.hashCode();
    }

//    public Set<Character> getExtensionKeys() {
    public Set getExtensionKeys() {
        if (_extMap != null) {
            return Collections.unmodifiableSet(_extMap.keySet());
        }
        return null;
    }

    public String getExtensionValue(char key) {
        if (_extMap != null) {
//            return _extMap.get(Character.valueOf(key));
            return (String)_extMap.get(new Character(key));
        }
        return null;
    }

//    public Set<String> getLDMLKeywordKeys() {
    public Set getLDMLKeywordKeys() {
        if (_kwdMap != null) {
            return Collections.unmodifiableSet(_kwdMap.keySet());
        }
        return null;
    }

    public String getLDMLKeywordType(String key) {
        if (key == null) {
            throw new NullPointerException("LDML key must not be null");
        }
        if (_kwdMap != null) {
//            return _kwdMap.get(key);
            return (String)_kwdMap.get(key);
        }
        return null;
    }

    public String getCanonicalString() {
        return _extensions;
    }

    public String toString() {
        return _extensions;
    }

//    private static String extensionsToCanonicalString(TreeMap<Character, String> extMap) {
    private static String extensionsToCanonicalString(TreeMap extMap) {
        if (extMap == null || extMap.size() == 0) {
            return "";
        }
//        StringBuilder canonicalbuf = new StringBuilder();
        StringBuffer canonicalbuf = new StringBuffer();
        String privUseStr = null;
        if (extMap != null) {
//          Set<Map.Entry<Character, String>> entries = extMap.entrySet();
//          for (Map.Entry<Character, String> entry : entries) {
//              Character key = entry.getKey();
//              String value = entry.getValue();
            Set entries = extMap.entrySet();
            Iterator itr = entries.iterator();
            while (itr.hasNext()) {
                Map.Entry entry = (Map.Entry)itr.next();
                Character key = (Character)entry.getKey();
                String value = (String)entry.getValue();

                if (key.charValue() == PRIVUSE.charAt(0)) {
                    privUseStr = value;
                    continue;
                }
                if (canonicalbuf.length() > 0) {
                    canonicalbuf.append(LOCALEEXTSEP);
                }
                canonicalbuf.append(key);
                canonicalbuf.append(LOCALEEXTSEP);
                canonicalbuf.append(value);
            }
        }
        if (privUseStr != null) {
            if (canonicalbuf.length() > 0) {
                canonicalbuf.append(LOCALEEXTSEP);
            }
            canonicalbuf.append(PRIVUSE);
            canonicalbuf.append(LOCALEEXTSEP);
            canonicalbuf.append(privUseStr);
        }
        return canonicalbuf.toString().intern();
    }

//    static void keywordsToString(TreeMap<String, String> map, StringBuilder buf) {
    public static void keywordsToString(TreeMap map, StringBuffer buf) {
//      Set<Map.Entry<String, String>> entries = map.entrySet();
//      for (Map.Entry<String, String> entry : entries) {
        Set entries = map.entrySet();
        Iterator itr = entries.iterator();
        while (itr.hasNext()) {
            Map.Entry entry = (Map.Entry)itr.next();

            if (buf.length() > 0) {
                buf.append(LOCALEEXTSEP);
            }
            buf.append(entry.getKey());
            buf.append(LOCALEEXTSEP);
            buf.append(entry.getValue());
        }
    }

    public static boolean isValidExtensionKey(char key) {
        return AsciiUtil.isAlphaNumeric(key);
    }

    public static boolean isValidLDMLKey(String key) {
        return (key.length() == 2) && AsciiUtil.isAlphaNumericString(key);
    }

    public static boolean isValidLDMLType(String type) {
        return (type.length() >= 3) && (type.length() <= 8) && AsciiUtil.isAlphaNumericString(type);
    }
}
