/*
 *******************************************************************************
 * Copyright (C) 2002-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;

public class FileUtilities {
    public static final boolean SHOW_FILES;
    static {
        boolean showFiles = false;
        try {
            showFiles = System.getProperty("SHOW_FILES") != null;
        } catch (SecurityException ignored) {
        }
        SHOW_FILES = showFiles;
    }

    public static final PrintWriter CONSOLE = new PrintWriter(System.out,true);

    private static PrintWriter log = CONSOLE;

    public static BufferedReader openUTF8Reader(String dir, String filename) throws IOException {
        return openReader(dir, filename, "UTF-8");
    }

    public static BufferedReader openReader(String dir, String filename, String encoding) throws IOException {
        File file = dir.length() == 0 ? new File(filename) : new File(dir, filename);
        if (SHOW_FILES && log != null) {
            log.println("Opening File: "
                + file.getCanonicalPath());
        }
        return new BufferedReader(
            new InputStreamReader(
                new FileInputStream(file),
                encoding),
            4*1024);
    }

    public static PrintWriter openUTF8Writer(String dir, String filename) throws IOException {
        return openWriter(dir, filename, "UTF-8");
    }

    public static PrintWriter openWriter(String dir, String filename, String encoding) throws IOException {
        File file = new File(dir, filename);
        if (SHOW_FILES && log != null) {
            log.println("Creating File: "
                + file.getCanonicalPath());
        }
        String parentName = file.getParent();
        if (parentName != null) {
            File parent = new File(parentName);
            parent.mkdirs();
        }
        return new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(file),
                    encoding),
                4*1024));
    }

    public static void appendFile(String filename, String encoding, PrintWriter output) throws IOException {
        appendFile(filename, encoding, output, null);
    }

    public static void appendFile(String filename, String encoding, PrintWriter output, String[] replacementList) throws IOException {
        BufferedReader br = openReader("", filename, encoding);
        /*
        FileInputStream fis = new FileInputStream(filename);
        InputStreamReader isr = (encoding == UTF8_UNIX || encoding == UTF8_WINDOWS) ? new InputStreamReader(fis, "UTF8") :  new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr, 32*1024);
        */
        try {
            appendBufferedReader(br, output, replacementList);
        } finally {
            br.close();
        }
    }

    public static void appendBufferedReader(BufferedReader br,
            PrintWriter output, String[] replacementList) throws IOException {
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
        br.close();
    }

    /**
     * Replaces all occurrences of piece with replacement, and returns new String
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
