/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import com.ibm.icu.impl.locale.LanguageTag.ParseStatus;

public class PrivateuseExtension extends Extension {
    public static final char SINGLETON = 'x';

    protected PrivateuseExtension() {
        super(SINGLETON);
    }

    /*
     * package local constructor only used by LanguageTag implementation
     */
    PrivateuseExtension(String privuse) {
        super(SINGLETON);
        _value = privuse;
    }

    protected void setExtensionValue(StringTokenIterator itr, ParseStatus sts) {
        if (sts.isError() || itr.isDone()) {
            _value = null;
            return;
        }

        StringBuilder buf = new StringBuilder();
        while (!itr.isDone()) {
            String s = itr.current();
            if (!LanguageTag.isPrivateuseSubtag(s)) {
                break;
            }
            s = LanguageTag.canonicalizePrivateuseSubtag(s);
            if (buf.length() != 0) {
                buf.append(LanguageTag.SEP);
            }
            buf.append(s);
            sts.parseLength = itr.currentEnd();
            itr.next();
        }

        if (buf.length() == 0) {
            _value = null;
        } else {
            _value = buf.toString();
        }
    }
}
