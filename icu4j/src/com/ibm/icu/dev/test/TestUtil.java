/**
 *******************************************************************************
 * Copyright (C) 2001-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/TestUtil.java,v $
 * $Date: 2003/06/03 18:49:28 $
 * $Revision: 1.5 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TestUtil {
    /**
     * Standard path to the test data.
     */
    public static final String DATA_PATH = "/src/com/ibm/icu/dev/data";

    /**
     * Property for user-defined data path.
     */
    public static final String DATA_PATH_PROPERTY = "ICUDataPath";


    /**
     * Property for modular build.
     */
    public static final String DATA_MODULAR_BUILD_PROPERTY = "ICUModularBuild";

    /**
     * Get path to test data.<p>
     *
     * path is provided relative to the src path, however the user could
     * set a system property to change the directory path.<br>
     */
    public static final File getDataPathRoot() {
        String s = System.getProperty(DATA_PATH_PROPERTY);
        if (s == null) {
            // assume user.dir is directly above src directory
            s = System.getProperty("user.dir");
            s = s + DATA_PATH;
        }

        File f = new File(s);
        if (!f.exists()) {
            throw new InternalError("cannot find ICU data root '" + f.getAbsolutePath() + "', try definining " + DATA_PATH_PROPERTY);
        }

        return f;
    }

    /**
     * Return the data file at path 'name' rooted at the data path.
     * For example, <pre>getDataFile("unicode/UnicodeData.txt");</pre>
     */
    public static final File getDataFile(String name) {
        File f = new File(getDataPathRoot(), name);
        if (!f.exists()) {
            throw new InternalError("cannot find ICU data file '" + f.getAbsolutePath() + "'");
        }

        return f;
    }

    /**
     * Return a buffered reader on the data file at path 'name' rooted at the data path
     * with initial buffer size 'bufSize'.
     */
    public static final BufferedReader getDataReader(String name, int bufSize) throws IOException {
        File f = getDataFile(name);
        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr, bufSize);
        return br;
    }

    /**
     * Return a buffered reader on the data file at path 'name' rooted at the data path.
     */
    public static final BufferedReader getDataReader(String name) throws IOException {
        return getDataReader(name, 1024);
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
