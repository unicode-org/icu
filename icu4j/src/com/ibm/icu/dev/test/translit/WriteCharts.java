/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/translit/WriteCharts.java,v $
 * $Date: 2001/11/13 02:50:11 $
 * $Revision: 1.6 $
 *
 *****************************************************************************************
 */
 
package com.ibm.test.translit;
import com.ibm.text.*;
import com.ibm.test.*;
import com.ibm.util.Utility;
//import java.text.*;
import java.util.*;
import java.io.*;

public class WriteCharts {
    public static void main(String[] args) throws IOException {
        if (false) testSet();
        String testSet = "";
        if (args.length == 0) args = all;
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
    
    public static void testSet() {
        UnicodeSet s = new UnicodeSet("[[\u0000-\u007E \u30A1-\u30FC \uFF61-\uFF9F\u3001\u3002][:Katakana:][:Mark:]]");
        int count = s.getRangeCount();
        for (int i = 0; i < count; ++i) {
            int start = s.getRangeStart(i);
            int end = s.getRangeEnd(i);
            System.out.println(Integer.toString(start,16) + ".." + Integer.toString(end,16));
        }
    }
    
    static final String[] all = {
        "Cyrillic-Latin", "Greek-Latin", 
        "el-Latin",
        "Devanagari-Tamil", "Devanagari-Latin", 
        "Katakana-Latin", "Hiragana-Latin", "Hangul-Latin"
    };
    
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
            target = "[:Latin:]";
        } else {
            target = "[:" + target + ":]";
        }
        UnicodeSet targetSet = new UnicodeSet(target);        
        
        Transliterator inverse = t.getInverse();
        
        Transliterator hex = Transliterator.getInstance("Any-Hex");
        
                
        // iterate through script
        System.out.println("Transliterating " + sourceSet.toPattern(true) 
            + " with " + Transliterator.getDisplayName(id));
                
        UnicodeSet leftOverSet = new UnicodeSet(targetSet);
        UnicodeSet privateUse = new UnicodeSet("[:private use:]");
            
        Map map = new TreeMap();
        
        UnicodeSet targetSetPlusAnyways = new UnicodeSet(targetSet);
        targetSetPlusAnyways.addAll(okAnyway);
        
        UnicodeSet sourceSetPlusAnyways = new UnicodeSet(sourceSet);
        sourceSetPlusAnyways.addAll(okAnyway);
                
        int count = sourceSet.getRangeCount();
        for (int i = 0; i < count; ++i) {
            int end = sourceSet.getRangeEnd(i);
            for (int j = sourceSet.getRangeStart(i); j <= end; ++j) {
                String flag = "";
                String ss = UTF16.valueOf(j);
                String ts = t.transliterate(ss);
                char group = 0;
                if (!containsAll(targetSetPlusAnyways, ts)) {
                    group |= 1;
                }
                if (UTF16.countCodePoint(ts) == 1) {
                    leftOverSet.remove(UTF16.charAt(ts,0));
                }
                String rt = inverse.transliterate(ts);
                if (!containsAll(sourceSetPlusAnyways, rt)) {
                    group |= 2;
                } else if (!ss.equals(rt)) {
                    group |= 4;
                }
                
                if (containsSome(privateUse, ts) || containsSome(privateUse, rt)) {
                    group |= 16;
                }
                    
                map.put(group + UCharacter.toLowerCase(Normalizer.normalize(ss, Normalizer.DECOMP_COMPAT, 0))
                        + "\u0000" + ss, 
                    "<tr><td>" + ss + "<br><tt>" + hex.transliterate(ss) + "</tt></td><td>"
                        + ts + "<br><tt>" + hex.transliterate(ts) + "</tt></td><td>"
                        + rt + "<br><tt>" + hex.transliterate(rt) + "</tt></td></tr>" );
            }
        }
        
        leftOverSet.remove(0x0100,0x02FF); // remove extended & IPA
        
        count = leftOverSet.getRangeCount();
        for (int i = 0; i < count; ++i) {
            int end = leftOverSet.getRangeEnd(i);
            for (int j = leftOverSet.getRangeStart(i); j <= end; ++j) {
                String ts = UTF16.valueOf(j);
                // String decomp = Normalizer.normalize(ts, Normalizer.DECOMP_COMPAT, 0);
                // if (!decomp.equals(ts)) continue;
                
                String rt = inverse.transliterate(ts);
                String flag = "";
                char group = 0x80;
                    
                if (!containsAll(sourceSetPlusAnyways, rt)) {
                    group |= 8;
                }
                if (containsSome(privateUse, rt)) {
                    group |= 16;
                }
                    
                map.put(group + UCharacter.toLowerCase(Normalizer.normalize(ts, Normalizer.DECOMP_COMPAT, 0)) + ts, 
                    "<tr><td>-</td><td>" + ts + "<br><tt>" + hex.transliterate(ts) + "</tt></td><td>"
                    + rt + "<br><tt>" + hex.transliterate(rt) + "</tt></td></tr>");
            }
        }

        // make file name and open
        File f = new File("chart_" + id.replace('/', '_') + ".html");
        String filename = f.getCanonicalFile().toString();
        PrintWriter out = new PrintWriter(
            new OutputStreamWriter(
                new FileOutputStream(filename), "UTF-8"));
        //out.print('\uFEFF'); // BOM
        
        System.out.println("Writing " + filename);
        
        try {
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
            out.println("<HTML><HEAD>");
            out.println("<META content=\"text/html; charset=utf-8\" http-equiv=Content-Type></HEAD>");
            out.println("<link rel='stylesheet' href='http://www.unicode.org/charts/uca/charts.css' type='text/css'>");
            
            out.println("<BODY>");
            String tableHeader = "<p><table border='1'><tr><th>Source</th><th>Target</th><th>Return</th></tr>";
            String tableFooter = "</table></p>";
            out.println("<h1>Round Trip</h1>");
            out.println(tableHeader);
            
            Iterator it = map.keySet().iterator();
            char lastGroup = 0;
            count = 0;
            while (it.hasNext()) {
                String key = (String) it.next();
                char group = key.charAt(0);
                if (group != lastGroup || count++ > 50) {
                    lastGroup = group;
                    count = 0;
                    out.println(tableFooter);
                    
                    String title = "";
                    if ((group & 0x80) != 0) out.println("<hr><h1>Completeness</h1>");
                    else out.println("<hr><h1>Round Trip</h1>");
                    if ((group & 16) != 0) out.println("<h2>Errors: Contains Private Use Characters</h2>");
                    if ((group & 8) != 0) out.println("<h2>Possible Errors: Return not in Source Set</h2>");
                    if ((group & 4) != 0) out.println("<h2>Errors: Return not equal to Source</h2>");
                    if ((group & 2) != 0) out.println("<h2>Errors: Return not in Source Set</h2>");
                    if ((group & 1) != 0) out.println("<h2>Errors: Target not in Target Set</h2>");
                                        
                    out.println(tableHeader);
                }
                String value = (String) map.get(key);
                out.println(value);
            }
            out.println(tableFooter + "</BODY></HTML>");
            
        } finally {
            out.close();
        }
    }
    
    static final UnicodeSet okAnyway = new UnicodeSet("[^[:Letter:]]");
    
    /*
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
    */
    
    // tests whether a string is in a set.
    public static boolean containsSome(UnicodeSet set, String s) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(i)) {
            cp = UTF16.charAt(s, i);
            if (set.contains(cp)) return true;
        }
        return false;
    }
    
    // tests whether a string is in a set.
    public static boolean containsAll(UnicodeSet set, String s) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(i)) {
            cp = UTF16.charAt(s, i);
            if (!set.contains(cp)) return false;
        }
        return true;
    }
    
    
}
  