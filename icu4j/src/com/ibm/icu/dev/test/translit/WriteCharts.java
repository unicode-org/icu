/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/translit/WriteCharts.java,v $
 * $Date: 2001/11/02 00:37:47 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
 
package com.ibm.test.translit;
import com.ibm.text.*;
import com.ibm.test.*;
import com.ibm.util.Utility;
import java.text.*;
import java.util.*;
import java.io.*;

public class WriteCharts {
    public static void main(String[] args) throws IOException {
        String testSet = "";
        for (int i = 0; i < args.length; ++i) {
    // Enumeration enum = Transliterator.getAvailableIDs();
            if (args[i].startsWith("[")) {
                testSet = args[i];
            } else {
                print(testSet, args[i]);
                testSet = "";
            }
        }
    }
    
    public static void print(String testSet, String rawId) throws IOException {
        Transliterator t = Transliterator.getInstance(rawId);
        String id = t.getID();
        
        // clean up IDs. Ought to be API for getting source, target, variant
        int minusPos = id.indexOf('-');
        String source = id.substring(0,minusPos);
        String target = id.substring(minusPos+1);
        int slashPos = target.indexOf('/');
        if (slashPos >= 0) target = target.substring(0,slashPos);
        
        // check that the source is a script
        if (testSet.equals("")) {
            int[] scripts = UScript.getCode(source);
            if (scripts.length != 1) {
                System.out.println("FAILED: " 
                    + Transliterator.getDisplayName(id)
                    + " does not have a script as the source");
                return;
            } else {
                testSet = "[:" + source + ":]";
            }
        }
        UnicodeSet sourceSet = new UnicodeSet(testSet);

        // check that the source is a script
        int[] scripts = UScript.getCode(target);
        if (scripts.length != 1) {
            target = "[a-zA-Z]";
        } else {
            target = "[:" + target + ":]";
        }
        UnicodeSet targetSet = new UnicodeSet(target);        
        
        Transliterator inverse = t.getInverse();
        
        // make file name and open
        File f = new File("chart_" + id.replace('/', '_') + ".txt");
        String filename = f.getCanonicalFile().toString();
        PrintWriter out = new PrintWriter(
            new OutputStreamWriter(
                new FileOutputStream(filename), "UTF-8"));
        out.print('\uFEFF'); // BOM
                
        // iterate through script
        try {
            System.out.println("Transliterating " + sourceSet.toPattern(true) 
                + " with " + Transliterator.getDisplayName(id)
                + " to " + filename);
                
            UnicodeSet leftOverSet = new UnicodeSet(targetSet);
            
            out.println("# Checking Round Trips for characters in source");
            out.println();
                
            int count = sourceSet.getRangeCount();
            for (int i = 0; i < count; ++i) {
                int end = sourceSet.getRangeEnd(i);
                for (int j = sourceSet.getRangeStart(i); j <= end; ++j) {
                    String flag = "";
                    String ss = UTF16.valueOf(j);
                    String ts = t.transliterate(ss);
                    if (!isIn(ts, targetSet)) flag += "Not in Target Set; ";
                    if (UTF16.countCodePoint(ts) == 1) {
                        leftOverSet.remove(UTF16.charAt(ts,0));
                    }
                    String rt = inverse.transliterate(ts);
                    if (!isIn(rt, sourceSet)) flag += "Not in Source Set; ";
                    else if (!ss.equals(rt)) flag = "NO Round Trip; ";
                    out.println(ss + "\t" + ts + "\t" + rt + "\t" + flag);
                }
            }
            
            out.println();
            out.println("# Checking fallbacks for characters in target");
            out.println();
            
            count = leftOverSet.getRangeCount();
            for (int i = 0; i < count; ++i) {
                int end = leftOverSet.getRangeEnd(i);
                for (int j = leftOverSet.getRangeStart(i); j <= end; ++j) {
                    String ts = UTF16.valueOf(j);
                    String rt = inverse.transliterate(ts);
                    String flag = "";
                    
                    if (!isIn(rt, sourceSet)) flag += "Not in Source Set; ";
                    out.println("-\t" + ts + "\t" + rt + "\t" + flag);
                }
            }
            
        } finally {
            out.close();
        }
    }
    
    static final UnicodeSet okAnyway = new UnicodeSet("[^[:Letter:]]");
    
    // tests whether a string is in a set. Also checks for Common and Inherited
    public static boolean isIn(String s, UnicodeSet set) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(i)) {
            cp = UTF16.charAt(s, i);
            if (set.contains(cp)) continue;
            if (okAnyway.contains(cp)) continue;
            return false;
        }
        return true;
    }
    
}
  