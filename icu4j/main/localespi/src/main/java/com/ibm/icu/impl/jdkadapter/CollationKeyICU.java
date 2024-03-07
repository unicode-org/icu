// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.jdkadapter;

import com.ibm.icu.text.CollationKey;

/**
 * CollationKeyICU is an adapter class which wraps ICU4J CollationKey and
 * implements java.text.CollationKey APIs.
 */
public class CollationKeyICU extends java.text.CollationKey {

    private CollationKey fIcuCollKey;

    private CollationKeyICU(CollationKey icuCollKey) {
        super(icuCollKey.getSourceString());
        fIcuCollKey = icuCollKey;
    }

    public static java.text.CollationKey wrap(CollationKey icuCollKey) {
        return new CollationKeyICU(icuCollKey);
    }

    public CollationKey unwrap() {
        return fIcuCollKey;
    }

    @Override
    public int compareTo(java.text.CollationKey target) {
        if (target instanceof CollationKeyICU) {
            return fIcuCollKey.compareTo(((CollationKeyICU)target).fIcuCollKey);
        }
        return 0;
    }

    @Override
    public String getSourceString() {
        return fIcuCollKey.getSourceString();
    }

    @Override
    public byte[] toByteArray() {
        return fIcuCollKey.toByteArray();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CollationKeyICU) {
            return ((CollationKeyICU)obj).fIcuCollKey.equals(fIcuCollKey);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return fIcuCollKey.hashCode();
    }
}
