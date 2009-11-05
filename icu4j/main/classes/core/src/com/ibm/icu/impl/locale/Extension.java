/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import com.ibm.icu.impl.locale.LanguageTag.ParseStatus;

public class Extension {
    private char _key;
    protected String _value;

    protected Extension(char key) {
        _key = key;
    }

    public char getKey() {
        return _key;
    }

    public String getValue() {
        return _value;
    }

    public String getID() {
        return _key + LanguageTag.SEP + _value;
    }

    public String toString() {
        return getID();
    }

    public static Extension create(StringTokenIterator itr, ParseStatus sts) {
        if (sts.isError() || itr.isDone()) {
            return null;
        }

        Extension ext = null;
        String key = itr.current();
        if (LanguageTag.isExtensionSingleton(key) || LanguageTag.isPrivateuseSingleton(key)) {
            itr.next();
            ext = create(key.charAt(0), itr, sts);
        }

        return ext;
    }

    public static Extension create(char key, StringTokenIterator val, ParseStatus sts) {
        if (sts.isError()) {
            return null;
        }
        if (val.isDone()) {
            sts.errorIndex = val.currentStart();
            sts.errorMsg = "Missing extension subtag for extension :" + key;
            return null;
        }

        Extension ext = null;
        key = AsciiUtil.toLower(key);

        switch (key) {
        case UnicodeLocaleExtension.SINGLETON:
            ext = new UnicodeLocaleExtension();
            break;
        case PrivateuseExtension.SINGLETON:
            ext = new PrivateuseExtension();
            break;
        default:
            ext = new Extension(key);
            break;
        }

        ext.setExtensionValue(val, sts);

        if (ext.getValue() == null) {
            // return null only when nothing parsed.
            return null;
        }

        return ext;
    }

    protected void setExtensionValue(StringTokenIterator itr, ParseStatus sts) {
        if (sts.isError() || itr.isDone()) {
            _value = null;
            return;
        }

        StringBuilder buf = new StringBuilder();
        while (!itr.isDone()) {
            String s = itr.current();
            if (!LanguageTag.isExtensionSubtag(s)) {
                break;
            }
            s = LanguageTag.canonicalizeExtensionSubtag(s);
            if (buf.length() != 0) {
                buf.append(LanguageTag.SEP);
            }
            buf.append(s);
            sts.parseLength = itr.currentEnd();
            itr.next();
        }

        if (buf.length() == 0) {
            sts.errorIndex = itr.currentStart();
            sts.errorMsg = "Invalid extension subtag: " + itr.current(); 
            _value = null;
        } else {
            _value = buf.toString();
        }
    }
}
