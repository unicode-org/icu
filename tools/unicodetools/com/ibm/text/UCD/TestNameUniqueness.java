/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/TestNameUniqueness.java,v $
* $Date: 2003/02/26 00:35:09 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.ibm.text.utility.*;
import com.ibm.icu.text.UnicodeSet;

public class TestNameUniqueness implements UCD_Types {
    
    public static void test() throws IOException {
        Default.setUCD();
        new TestNameUniqueness().checkNames();
    }
    
    Map names = new HashMap();
    int[] charCount = new int[128];
    int[] samples = new int[128];
    
    void checkNames() throws IOException {
        PrintWriter out = Utility.openPrintWriter("name_uniqueness.txt", Utility.LATIN1_WINDOWS);
        try {
            out.println("Collisions");
            out.println();
            for (int cp = 0; cp < 0x10FFFF; ++cp) {
                Utility.dot(cp);
                if (!Default.ucd.isAllocated(cp)) continue;
                if (Default.ucd.hasComputableName(cp)) continue;
                int cat = Default.ucd.getCategory(cp);
                if (cat == Cc) continue;
                
                String name = Default.ucd.getName(cp);
                String processedName = processName(cp, name);
                Integer existing = (Integer) names.get(processedName);
                if (existing != null) {
                    out.println("Collision between: "
                        + Default.ucd.getCodeAndName(existing.intValue())
                        + ", " + Default.ucd.getCodeAndName(cp));
                } else {
                    names.put(processedName, new Integer(cp));
                }
            }
            out.println();
            out.println("Samples");
            out.println();
            for (int i = 0; i < charCount.length; ++i) {
                int count = charCount[i];
                if (count == 0) continue;
                String sampleName = Default.ucd.getCodeAndName(samples[i]);
                out.println(count + "\t'" + ((char)i)
                    + "'\t" + Default.ucd.getCodeAndName(samples[i])
                    + "\t=>\t" + processName(samples[i], Default.ucd.getName(samples[i])));
            }
            out.println();
            out.println("Name Samples");
            out.println();
            for (int i = 0; i < 256; ++i) {
                int cat = Default.ucd.getCategory(i);
                if (cat == Cc) continue;
                out.println(Default.ucd.getCodeAndName(i)
                    + "\t=>\t" + processName(i, Default.ucd.getName(i)));
            }
        } finally {
            out.close();
        }
    }
    
    static final String[][] replacements = {
        //{"SMALL LETTER", ""},
        {"LETTER", ""},
        {"CHARACTER", ""},
        {"DIGIT", ""},
        {"SIGN", ""},
        //{"WITH", ""},
    };
    
    StringBuffer processNamesBuffer = new StringBuffer();
    
    String processName(int codePoint, String name) {
        name = Utility.replace(name, replacements);
        processNamesBuffer.setLength(0);
        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            ++charCount[c];
            if (samples[c] == 0) samples[c] = codePoint;
            if ('A' <= c && c <= 'Z'
                || '0' <= c && c <= '9') processNamesBuffer.append(c);
            
        }
        if (processNamesBuffer.length() == name.length()) return name;
        return processNamesBuffer.toString();
    }
}

