/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import com.ibm.icu.util.VersionInfo;

public final class ICUDebug {
    private static String params;
    static {
    try {
        params = System.getProperty("ICUDebug");
    }
    catch (SecurityException e) {
    }
    }
    private static boolean debug = params != null;
    private static boolean help = debug && (params.equals("") || params.indexOf("help") != -1);

    static {
    if (debug) {
        System.out.println("\nICUDebug=" + params);
    }
    }

    public static final String javaVersionString = System.getProperty("java.version");
    public static final boolean isJDK14OrHigher;
    public static final VersionInfo javaVersion;

    public static VersionInfo getInstanceLenient(String s) {
    // clean string
    // preserve only digits, separated by single '.' 
    // ignore over 4 digit sequences
    // does not test < 255, very odd...

    char[] chars = s.toCharArray();
    int r = 0, w = 0, count = 0;
    boolean numeric = false; // ignore leading non-numerics
    while (r < chars.length) {
        char c = chars[r++];
        if (c < '0' || c > '9') {
        if (numeric) {
            if (count == 3) {
            // only four digit strings allowed
            break;
            }
            numeric = false;
            chars[w++] = '.';
            ++count;
        }
        } else {
        numeric = true;
        chars[w++] = c;
        }
    }
    while (w > 0 && chars[w-1] == '.') {
        --w;
    }
    
    String vs = new String(chars, 0, w);

    return VersionInfo.getInstance(vs);
    }

    static {
    javaVersion = getInstanceLenient(javaVersionString);

        VersionInfo java14Version = VersionInfo.getInstance("1.4.0");

        isJDK14OrHigher = javaVersion.compareTo(java14Version) >= 0;
    }

    public static boolean enabled() {
    return debug;
    }

    public static boolean enabled(String arg) {
    if (debug) {
        boolean result = params.indexOf(arg) != -1;
        if (help) System.out.println("\nICUDebug.enabled(" + arg + ") = " + result);
        return result;
    }
    return false;
    }

    public static String value(String arg) {
    String result = "false";
    if (debug) {
        int index = params.indexOf(arg);
        if (index != -1) {
        index += arg.length();
        if (params.length() > index && params.charAt(index) == '=') {
            index += 1;
            int limit = params.indexOf(",", index);
            result = params.substring(index, limit == -1 ? params.length() : limit);
        }
        result = "true";
        }

        if (help) System.out.println("\nICUDebug.value(" + arg + ") = " + result);
    }
    return result;
    }

    static public void main(String[] args) {
    // test
    String[] tests = {
        "1.3.0",
        "1.3.0_02",
        "1.3.1ea",
        "1.4.1b43",
        "___41___5",
        "x1.4.51xx89ea.7f"
    };
    for (int i = 0; i < tests.length; ++i) {
        System.out.println(tests[i] + " => " + getInstanceLenient(tests[i]));
    }
    }
}
