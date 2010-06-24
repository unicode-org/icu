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

import com.ibm.icu.impl.locale.LanguageTag.ParseStatus;

public class UnicodeLocaleExtension extends Extension {
    public static final char SINGLETON = 'u';

    public static final UnicodeLocaleExtension CA_JAPANESE = new UnicodeLocaleExtension().put("ca", "japanese");
    public static final UnicodeLocaleExtension NU_THAI = new UnicodeLocaleExtension().put("nu", "thai");

    private SortedMap<String, String> _keyTypeMap;

    protected UnicodeLocaleExtension() {
        super(SINGLETON);
    }

    /*
     * Package local constructor only used by InternalLocaleBuilder
     */
    UnicodeLocaleExtension(SortedMap<String, String> keyTypeMap) {
        super(SINGLETON);
        _keyTypeMap = keyTypeMap;
        updateStringValue();
    }

    protected void setExtensionValue(StringTokenIterator itr, ParseStatus sts) {
        if (sts.isError() || itr.isDone()) {
            _value = null;
            return;
        }

        SortedMap<String, String> keyTypeMap = new TreeMap<String, String>();
        String ukey = null;
        StringBuilder buf = new StringBuilder();
        int typeEnd = -1;

        while (!itr.isDone()) {
            String s = itr.current();

            if (isTypeSubtag(s)) {
                if (ukey == null) {
                    // key is expected
                    sts.errorIndex = itr.currentStart();
                    sts.errorMsg = "Invalid Unicode locale extension key: " + s;
                    break;
                }
                if (buf.length() > 0) {
                    buf.append(LanguageTag.SEP);
                }
                buf.append(canonicalizeTypeSubtag(s));
                typeEnd = itr.currentEnd();

                if (!itr.hasNext()) {
                    // emit the last key/type
                    keyTypeMap.put(ukey, buf.toString());
                    sts.parseLength = typeEnd;
                    itr.next();
                    break;
                }
            } else {
                // key or others
                if (ukey != null) {
                    if (buf.length() > 0) {
                        // emit previous key and value
                        keyTypeMap.put(ukey, buf.toString());
                        sts.parseLength = typeEnd;
                    } else {
                        // type is expected
                        sts.errorIndex = itr.currentStart();
                        sts.errorMsg = "Invalid Unicode locale extension type: " + s;
                        break;
                    }
                }
                if (isKey(s)) {
                    if (itr.hasNext()) {
                        ukey = canonicalizeKey(s);
                        if (keyTypeMap.containsKey(ukey)) {
                            // duplicated key
                            sts.errorIndex = itr.currentStart();
                            sts.errorMsg = "Duplicate Unicode locale extension key: " + s;
                            break;
                        }
                        buf.setLength(0);
                        typeEnd = -1;
                    } else {
                        // missing type
                        sts.errorIndex = itr.currentStart();
                        sts.errorMsg = "Missing subtag for Unicode locale extension: " + s;
                        itr.next();
                        break;
                    }
                } else {
                    // others
                    if (keyTypeMap.size() == 0) {
                        // key is expected
                        sts.errorIndex = itr.currentStart();
                        sts.errorMsg = "Invalid Unicode locale extension key: " + s;
                    }
                    break;
                }
            }
            itr.next();
        }

        if (keyTypeMap.size() == 0) {
            _value = null;
            return;
        }

        _keyTypeMap = keyTypeMap;
        updateStringValue();
    }

    public Set<String> getKeys() {
        if (_keyTypeMap == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(_keyTypeMap.keySet());
    }

    public String getType(String key) {
        String type = null;
        if (_keyTypeMap != null) {
            type = _keyTypeMap.get(canonicalizeKey(key));
        }

        return (type == null ? "" : type);
    }

    public static boolean isKey(String s) {
        // 2alphanum
        return (s.length() == 2) && AsciiUtil.isAlphaNumericString(s);
    }

    public static boolean isTypeSubtag(String s) {
        // 3*8alphanum
        return (s.length() >= 3) && (s.length() <= 8) && AsciiUtil.isAlphaNumericString(s);
    }

    public static String canonicalizeKey(String s) {
        return LanguageTag.canonicalizeExtensionSubtag(s);
    }

    public static String canonicalizeTypeSubtag(String s) {
        return LanguageTag.canonicalizeExtensionSubtag(s);
    }

    // These methods are only used by InterlaLocaleBuilder
    UnicodeLocaleExtension remove(String key) {
        if (_keyTypeMap != null) {
            _keyTypeMap.remove(key);
            updateStringValue();
        }
        return this;
    }

    UnicodeLocaleExtension put(String key, String type) {
        if (_keyTypeMap == null) {
            _keyTypeMap = new TreeMap<String, String>();
        }
        _keyTypeMap.put(key, type);
        updateStringValue();
        return this;
    }

    boolean isEmpty() {
        return (_keyTypeMap.size() == 0);
    }

    private void updateStringValue() {
        _value = null;

        if (_keyTypeMap != null) {
            // re-construct string representation
            StringBuilder valBuf = new StringBuilder();
            Set<Entry<String, String>> entries = _keyTypeMap.entrySet();
            boolean isFirst = true;
            for (Entry<String, String> e : entries) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    valBuf.append(LanguageTag.SEP);
                }
                valBuf.append(e.getKey());
                valBuf.append(LanguageTag.SEP);
                valBuf.append(e.getValue());
            }

            if (valBuf.length() > 0) {
                _value = valBuf.toString();
            }
        }
    }
}

