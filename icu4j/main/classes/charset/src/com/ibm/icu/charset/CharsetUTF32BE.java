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
 * The purpose of this class is to set isBigEndian to true and isEndianSpecified to true in the super class, and to
 * allow the Charset framework to open the variant UTF-32 converter without extra setup work.
 */
class CharsetUTF32BE extends CharsetUTF32 {
    public CharsetUTF32BE(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
    }
}
