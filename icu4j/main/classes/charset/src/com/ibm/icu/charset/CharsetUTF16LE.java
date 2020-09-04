// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2006-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.charset;

/**
 * The purpose of this class is to set isBigEndian to false and isEndianSpecified to true in the super class, and to
 * allow the Charset framework to open the variant UTF-16 converter without extra setup work.
 */
class CharsetUTF16LE extends CharsetUTF16 {
    public CharsetUTF16LE(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
    }
}
