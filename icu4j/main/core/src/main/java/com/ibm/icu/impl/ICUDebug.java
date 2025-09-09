// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2001-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

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

    public static boolean enabled() {
        return debug;
    }

    /**
     * Returns true if a certain named debug flag is enabled.
     *
     * <p>
     * To enable debugging when running maven one must define {@code ICUDebug}.
     * For example this runs all the tests in {@code LocaleMatcherTest} without debugging:
     * <blockquote><pre>
     *     mvn package -f main/core/ -Dtest=LocaleMatcherTest
     * </pre></blockquote>
     * And this runs the same tests, but with debugging enabled:
     * <blockquote><pre>
     *     mvn package -f main/core/ -Dtest=LocaleMatcherTest -DICUDebug=localematchertest
     * </pre></blockquote>
     * You must check what name is used for the debugging flag by inspecting the code,
     * it is not always the lowercase class name.
     * 
     * @param name the name if the debug flag to enable
     * @return true if the debugging should be enabled for that flag
     */
    public static boolean enabled(String name) {
        if (debug) {
            boolean result = params.indexOf(name) != -1;
            if (help) System.out.println("\nICUDebug.enabled(" + name + ") = " + result);
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
                } else {
                    result = "true";
                }
            }

            if (help) System.out.println("\nICUDebug.value(" + arg + ") = " + result);
        }
        return result;
    }
}
