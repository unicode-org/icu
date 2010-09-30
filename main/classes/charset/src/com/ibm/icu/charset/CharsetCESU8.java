/**
 *******************************************************************************
 * Copyright (C) 2006-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.charset;

import com.ibm.icu.text.UnicodeSet;

/**
 * The purpose of this class is to set isCESU8 to true in the super class, and to allow the Charset framework to open
 * the variant UTF-8 converter without extra setup work. CESU-8 encodes/decodes supplementary characters as 6 bytes
 * instead of the proper 4 bytes.
 */
class CharsetCESU8 extends CharsetUTF8 {
    public CharsetCESU8(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
    }
    
    
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        getCompleteUnicodeSet(setFillIn);
            
    }
}
