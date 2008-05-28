//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 2002-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

public class FileUtilities {
    public static void appendFile(String filename, String encoding, PrintWriter output) throws IOException {
        appendFile(filename, encoding, output, null);
    }
    
    public static void appendFile(String filename, String encoding, PrintWriter output, String[] replacementList) throws IOException {
        BufferedReader br = BagFormatter.openReader("", filename, encoding);
        /*
        FileInputStream fis = new FileInputStream(filename);
        InputStreamReader isr = (encoding == UTF8_UNIX || encoding == UTF8_WINDOWS) ? new InputStreamReader(fis, "UTF8") :  new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr, 32*1024);
        */
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            if (replacementList != null) {
                for (int i = 0; i < replacementList.length; i += 2) {
                    line = replace(line, replacementList[i], replacementList[i+1]);
                }
            }
            output.println(line);
        }
    }

    /**
     * Replaces all occurances of piece with replacement, and returns new String
     */
    public static String replace(String source, String piece, String replacement) {
        if (source == null || source.length() < piece.length()) return source;
        int pos = 0;
        while (true) {
            pos = source.indexOf(piece, pos);
            if (pos < 0) return source;
            source = source.substring(0,pos) + replacement + source.substring(pos + piece.length());
            pos += replacement.length();
        }
    }
    
    public static String replace(String source, String[][] replacements) {
        return replace(source, replacements, replacements.length);
    }    
    
    public static String replace(String source, String[][] replacements, int count) {
        for (int i = 0; i < count; ++i) {
            source = replace(source, replacements[i][0], replacements[i][1]);
        }
        return source;
    }    
    
    public static String replace(String source, String[][] replacements, boolean reverse) {
        if (!reverse) return replace(source, replacements);
        for (int i = 0; i < replacements.length; ++i) {
            source = replace(source, replacements[i][1], replacements[i][0]);
        }
        return source;
    }
    
    public static String anchorize(String source) {
        String result = source.toLowerCase(Locale.ENGLISH).replaceAll("[^\\p{L}\\p{N}]+", "_");
        if (result.endsWith("_")) result = result.substring(0,result.length()-1);
        if (result.startsWith("_")) result = result.substring(1);
        return result;
    }
}
//#endif
