/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

public final class AsciiUtil {
    public static boolean caseIgnoreMatch(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        int len = s1.length();
        if (len != s2.length()) {
            return false;
        }
        int i = 0;
        while (i < len) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            if (c1 != c2 && toLower(c1) != toLower(c2)) {
                break;
            }
            i++;
        }
        return (i == len);
    }

    public static int caseIgnoreCompare(String s1, String s2) {
        if (s1 == s2) {
            return 0;
        }
        return AsciiUtil.toLowerString(s1).compareTo(AsciiUtil.toLowerString(s2));
    }


    public static char toUpper(char c) {
        if (c >= 'a' && c <= 'z') {
            c -= 0x20;
        }
        return c;
    }

    public static char toLower(char c) {
        if (c >= 'A' && c <= 'Z') {
            c += 0x20;
        }
        return c;
    }

    public static String toLowerString(String s) {
        int idx = 0;
        for (; idx < s.length(); idx++) {
            char c = s.charAt(idx);
            if (c >= 'A' && c <= 'Z') {
                break;
            }
        }
        if (idx == s.length()) {
            return s;
        }
//        StringBuilder buf = new StringBuilder(s.substring(0, idx));
        StringBuffer buf = new StringBuffer(s.substring(0, idx));
        for (; idx < s.length(); idx++) {
            buf.append(toLower(s.charAt(idx)));
        }
        return buf.toString();
    }

    public static String toUpperString(String s) {
        int idx = 0;
        for (; idx < s.length(); idx++) {
            char c = s.charAt(idx);
            if (c >= 'a' && c <= 'z') {
                break;
            }
        }
        if (idx == s.length()) {
            return s;
        }
//        StringBuilder buf = new StringBuilder(s.substring(0, idx));
        StringBuffer buf = new StringBuffer(s.substring(0, idx));
        for (; idx < s.length(); idx++) {
            buf.append(toUpper(s.charAt(idx)));
        }
        return buf.toString();
    }

    public static boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    public static boolean isAlphaString(String s) {
        boolean b = true;
        for (int i = 0; i < s.length(); i++) {
            if (!isAlpha(s.charAt(i))) {
                b = false;
                break;
            }
        }
        return b;
    }

    public static boolean isNumeric(char c) {
        return (c >= '0' && c <= '9');
    }

    public static boolean isNumericString(String s) {
        boolean b = true;
        for (int i = 0; i < s.length(); i++) {
            if (!isNumeric(s.charAt(i))) {
                b = false;
                break;
            }
        }
        return b;
    }

    public static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isNumeric(c);
    }

    public static boolean isAlphaNumericString(String s) {
        boolean b = true;
        for (int i = 0; i < s.length(); i++) {
            if (!isAlphaNumeric(s.charAt(i))) {
                b = false;
                break;
            }
        }
        return b;
    }
}
