/**
 *******************************************************************************
 * Copyright (C) 2001-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/TestUtil.java,v $
 * $Date: 2003/11/17 23:48:19 $
 * $Revision: 1.8 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestUtil {
    /**
     * Standard path to the test data.
     */
    public static final String DATA_PATH = "/src/com/ibm/icu/dev/data/";

    /**
     * Property for user-defined data path.
     */
    public static final String DATA_PATH_PROPERTY = "ICUDataPath";


    /**
     * Property for modular build.
     */
    public static final String DATA_MODULAR_BUILD_PROPERTY = "ICUModularBuild";

    /**
     * Compute a full data path using the ICUDataPath, if defined, or the user.dir, if we
     * are allowed access to it.
     */
    private static final String dataPath(String fileName) {
        String s = System.getProperty(DATA_PATH_PROPERTY);
        if (s == null) {
            // assume user.dir is directly above src directory
	    // data path must end in '/' or '\', fileName should not start with one
            s = System.getProperty("user.dir"); // protected property
            s = s + DATA_PATH;
        }
	return s + fileName;
    }

    /**
     * Return a buffered reader on the data file at path 'name' rooted at the data path.
     */
    public static final BufferedReader getDataReader(String name) throws IOException {
	InputStream is = new FileInputStream(dataPath(name));
	InputStreamReader isr = new InputStreamReader(is);
	return new BufferedReader(isr);
    }

    /**
     * Return a buffered reader on the data file at path 'name' rooted at the data path,
     * using the provided encoding.
     */
    public static final BufferedReader getDataReader(String name, String charset) throws IOException {
	InputStream is = new FileInputStream(dataPath(name));
	InputStreamReader isr = new InputStreamReader(is, charset);
	return new BufferedReader(isr);
    }

    /**
     * Return an input stream on the data file at path 'name' rooted at the data path
     */
    public static final InputStream getDataStream(String name) throws IOException{
        return new FileInputStream(dataPath(name));
    }

    static final char DIGITS[] = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X', 'Y', 'Z'
    };
    /**
     * Return true if the character is NOT printable ASCII.  The tab,
     * newline and linefeed characters are considered unprintable.
     */
    public static boolean isUnprintable(int c) {
        return !(c >= 0x20 && c <= 0x7E);
    }
    /**
     * Escape unprintable characters using <backslash>uxxxx notation
     * for U+0000 to U+FFFF and <backslash>Uxxxxxxxx for U+10000 and
     * above.  If the character is printable ASCII, then do nothing
     * and return FALSE.  Otherwise, append the escaped notation and
     * return TRUE.
     */
    public static boolean escapeUnprintable(StringBuffer result, int c) {
        if (isUnprintable(c)) {
            result.append('\\');
            if ((c & ~0xFFFF) != 0) {
                result.append('U');
                result.append(DIGITS[0xF&(c>>28)]);
                result.append(DIGITS[0xF&(c>>24)]);
                result.append(DIGITS[0xF&(c>>20)]);
                result.append(DIGITS[0xF&(c>>16)]);
            } else {
                result.append('u');
            }
            result.append(DIGITS[0xF&(c>>12)]);
            result.append(DIGITS[0xF&(c>>8)]);
            result.append(DIGITS[0xF&(c>>4)]);
            result.append(DIGITS[0xF&c]);
            return true;
        }
        return false;
    }

}
