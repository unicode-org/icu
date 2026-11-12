// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.samples.util.versioninfo;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.VersionInfo;

/** com.ibm.icu.util.VersionInfo Coding Examples */
public class VersionInfoExample {
    /** Main method equivalent to former VersionInfo main method, which was removed in ICU 80. */
    public static void main(String[] args) {
        String icuApiVer = getVersionString(VersionInfo.ICU_VERSION, 2, 2);
        String icuImplVer = getVersionString(VersionInfo.ICU_VERSION, 2, 4);
        String uniDataVer = getVersionString(UCharacter.getUnicodeVersion(), 2, 4);
        String cldrVer = getVersionString(LocaleData.getCLDRVersion(), 2, 4);
        String tzdataVer = TimeZone.getTZDataVersion();

        System.out.println("International Components for Unicode for Java " + icuApiVer);
        System.out.println("");
        System.out.println("Implementation Version: " + icuImplVer);
        System.out.println("Unicode Data Version:   " + uniDataVer);
        System.out.println("CLDR Data Version:      " + cldrVer);
        System.out.println("Time Zone Data Version: " + tzdataVer);
    }

    /**
     * Generate version string separated by dots with the specified digit width. Version digit 0
     * after <code>minDigits</code> will be trimmed off.
     */
    private static String getVersionString(VersionInfo vinf, int minDigits, int maxDigits) {
        if (minDigits < 1
                || maxDigits < 1
                || minDigits > 4
                || maxDigits > 4
                || minDigits > maxDigits) {
            throw new IllegalArgumentException("Invalid min/maxDigits range");
        }

        int[] digits = new int[4];
        digits[0] = vinf.getMajor();
        digits[1] = vinf.getMinor();
        digits[2] = vinf.getMilli();
        digits[3] = vinf.getMicro();

        int numDigits = maxDigits;
        while (numDigits > minDigits) {
            if (digits[numDigits - 1] != 0) {
                break;
            }
            numDigits--;
        }

        StringBuilder verStr = new StringBuilder(7);
        verStr.append(digits[0]);
        for (int i = 1; i < numDigits; i++) {
            verStr.append(".");
            verStr.append(digits[i]);
        }

        return verStr.toString();
    }
}
