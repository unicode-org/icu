/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/TestNormalization.java,v $
* $Date: 2002/06/13 21:14:05 $
* $Revision: 1.5 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;

import com.ibm.text.utility.*;

public final class TestNormalization {
    static final String DIR = "C:\\Documents and Settings\\Davis\\My Documents\\UnicodeData\\Update 3.0.1\\";
    static final boolean SKIP_FILE = true;

    static PrintWriter out = null;
    static BufferedReader in = null;

    static BitSet charsListed = new BitSet(0x110000);
    static int errorCount = 0;
    static int lineErrorCount = 0;
    static String originalLine = "";
    static String lastLine = "";

    public static void main(String[] args)  throws java.io.IOException {
        System.out.println("Creating Normalizers");
        Default.setUCD();
        
        String[] testSet = {"a\u0304\u0328", "a\u0328\u0304"};
        for (int i = 0; i < testSet.length; ++i) {
            String s = testSet[i];
            boolean test = Default.nfc.isFCD(s);
            System.out.println(test + ": " + Default.ucd.getCodeAndName(s));
        }


            String x = UTF32.valueOf32(0x10000);
            check("NFC", Default.nfc, x);
            check("NFD", Default.nfd, x);
            check("NFKC", Default.nfkc, x);
            check("NFKD", Default.nfkd, x);


        out = new PrintWriter(
            new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream("NormalizationTestLog.txt"),
                "UTF8"),
            32*1024));

        in = new BufferedReader (
            new FileReader (DIR + "NormalizationTest.txt"),
            32*1024);

        try {
            String[] parts = new String[10];

            System.out.println("Checking files");

            int count = 0;

            while (true) {
                String line = in.readLine();
                if ((count++ & 0x3FF) == 0) System.out.println("#LINE: " + line);
                if (line == null) break;
                originalLine = line;
                int pos = line.indexOf('#');
                if (pos >= 0) {
                    line = line.substring(0,pos);
                }
                line = line.trim();
                if (line.length() == 0) continue;


                int splitCount = Utility.split(line, ';', parts);
                // FIX check splitCount
                for (int i = 0; i < splitCount; ++i) {
                    parts[i] = Utility.fromHex(parts[i]);
                }

                if (UTF32.length32(parts[0]) == 1) {
                    int code = UTF32.char32At(parts[0],0);
                    charsListed.set(code);
                    if ((code & 0x3FF) == 0) System.out.println("# " + Utility.hex(code));
                }

                // c2 == NFC(c1) == NFC(c2) == NFC(c3)
                errorCount += check("NFCa", Default.nfc, parts[1], parts[0]);
                errorCount += check("NFCb", Default.nfc, parts[1], parts[1]);
                errorCount += check("NFCc", Default.nfc, parts[1], parts[2]);

                // c4 == NFC(c4) == NFC(c5)
                errorCount += check("NFCd", Default.nfc, parts[3], parts[3]);
                errorCount += check("NFCe", Default.nfc, parts[3], parts[4]);

                // c3 == NFD(c1) == NFD(c2) == NFD(c3)
                errorCount += check("NFDa", Default.nfd, parts[2], parts[0]);
                errorCount += check("NFDb", Default.nfd, parts[2], parts[1]);
                errorCount += check("NFDc", Default.nfd, parts[2], parts[2]);

                // c5 == NFD(c4) == NFD(c5)
                errorCount += check("NFDd", Default.nfd, parts[4], parts[3]);
                errorCount += check("NFDe", Default.nfd, parts[4], parts[4]);

                // c4 == NFKC(c1) == NFKC(c2) == NFKC(c3) == NFKC(c4) == NFKC(c5)
                errorCount += check("NFKCa", Default.nfkc, parts[3], parts[0]);
                errorCount += check("NFKCb", Default.nfkc, parts[3], parts[1]);
                errorCount += check("NFKCc", Default.nfkc, parts[3], parts[2]);
                errorCount += check("NFKCd", Default.nfkc, parts[3], parts[3]);
                errorCount += check("NFKCe", Default.nfkc, parts[3], parts[4]);

                // c5 == NFKD(c1) == NFKD(c2) == NFKD(c3) == NFKD(c4) == NFKD(c5)
                errorCount += check("NFKDa", Default.nfkd, parts[4], parts[0]);
                errorCount += check("NFKDb", Default.nfkd, parts[4], parts[1]);
                errorCount += check("NFKDc", Default.nfkd, parts[4], parts[2]);
                errorCount += check("NFKDd", Default.nfkd, parts[4], parts[3]);
                errorCount += check("NFKDe", Default.nfkd, parts[4], parts[4]);
            }
            System.out.println("Total errors in file: " + errorCount
                + ", lines: " + lineErrorCount);
            errorCount = lineErrorCount = 0;

            System.out.println("Checking Missing");
            checkMissing();
            System.out.println("Total errors in unlisted items: " + errorCount
                + ", lines: " + lineErrorCount);

        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    static String lastBase = "";

    public static int check(String type, Normalizer n, String base, String other) {
        try {
            String trans = n.normalize(other);
            if (!trans.equals(base)) {
                String temp = "";
                if (!lastLine.equals(originalLine)) {
                    temp = "// " + originalLine;
                    lastLine = originalLine;
                }
                if (!base.equals(lastBase)) {
                    lastBase = base;
                    lineErrorCount++;
                }
                String otherList = "";
                if (!base.equals(other)) {
                    otherList = "(" + Default.ucd.getCodeAndName(other) + ")";
                }
                out.println("DIFF " + type + ": "
                    + Default.ucd.getCodeAndName(base) + " != "
                    + type
                    + otherList
                    + " == " + Default.ucd.getCodeAndName(trans)
                    + temp
                );
                return 1;
            }
        } catch (Exception e) {
            throw new ChainException("DIFF " + type + ": "
                + Default.ucd.getCodeAndName(base) + " != "
                + type + "(" + Default.ucd.getCodeAndName(other) + ")", new Object[]{}, e);
        }
        return 0;
    }

    public static int check(String type, Normalizer n, String base) {
        return check(type, n, base, base);
    }

    static void checkMissing() {
        for (int missing = 0; missing < 0x100000; ++missing) {
            if ((missing & 0xFFF) == 0) System.out.println("# " + Utility.hex(missing));
            if (charsListed.get(missing)) continue;
            String x = UTF32.valueOf32(missing);
            errorCount += check("NFC", Default.nfc, x);
            errorCount += check("NFD", Default.nfd, x);
            errorCount += check("NFKC", Default.nfkc, x);
            errorCount += check("NFKD", Default.nfkd, x);
        }
    }

}
