/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/29/2001  aliu        Creation.
**********************************************************************
*/
package com.ibm.icu.text;
import java.util.*;
import com.ibm.icu.dev.tool.translit.UnicodeSetClosure;
import java.io.*;

/**
 * This is a small class that resides in the com.ibm.icu.text package in
 * order to access some package-private API.  It is used for
 * development purposes and should be ignored by end clients.
 * To run, use:
 * java -classpath classes com.ibm.icu.text.TransliteratorUtility Latin-Katakana NFD lower
 * Output is produced in the command console, and a file with more detail is also written.
 * To see if it works, use:
 * java -classpath classes com.ibm.icu.dev.test.translit.TransliteratorTest -v -nothrow TestIncrementalProgress
 * and 
 * java -classpath classes com.ibm.icu.dev.demo.translit.Demo
 */
public class TransliteratorUtility {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            // Compute and display the source sets for all system
            // transliterators.
            for (Enumeration e = Transliterator.getAvailableIDs(); e.hasMoreElements(); ) {
                String ID = (String) e.nextElement();
                showSourceSet(ID, Normalizer.NONE, false);
            }
        } else {
            // Usage: ID [NFKD | NFD] [lower]
            Normalizer.Mode m = Normalizer.NONE;
            boolean lowerFirst = false;
            if (args.length >= 2) {
                if (args[1].equalsIgnoreCase("NFD")) {
                    m = Normalizer.NFD;
                } else if (args[1].equalsIgnoreCase("NFKD")) {
                    m = Normalizer.NFKD;
                } else {
                    usage();
                }
            }
            if (args.length >= 3) {
                if (args[2].equalsIgnoreCase("lower")) {
                    lowerFirst = true;
                } else {
                    usage();
                }
            }
            if (args.length > 3) {
                usage();
            }
            showSourceSet(args[0], m, lowerFirst);
        }
    }

    /**
     * Hook to allow tools to access package private method.
     */
    public static UnicodeSet getSourceSet(Transliterator t) {
        return t.getSourceSet();
    }

    static void showSourceSet(String ID, Normalizer.Mode m, boolean lowerFirst) throws IOException {
        File f = new File("UnicodeSetClosure.txt");
        String filename = f.getCanonicalFile().toString();
        out = new PrintWriter(
            new OutputStreamWriter(
                new FileOutputStream(filename), "UTF-8"));
        out.print('\uFEFF'); // BOM
        System.out.println();
        System.out.println("Writing " + filename);
        Transliterator t = Transliterator.getInstance(ID);
        showSourceSetAux(t, m, lowerFirst, true);
        showSourceSetAux(t.getInverse(), m, lowerFirst, false);
        out.close();
    }
    
    static PrintWriter out;
    
    static void showSourceSetAux(Transliterator t, Normalizer.Mode m, boolean lowerFirst, boolean forward) throws IOException {
        UnicodeSet sourceSet = t.getSourceSet();
        if (m != Normalizer.NONE || lowerFirst) {
            UnicodeSetClosure.close(sourceSet, m, lowerFirst);
        }
        System.out.println(t.getID() + ": " +
                           sourceSet.toPattern(true));
        out.println("# MINIMAL FILTER GENERATED FOR: " + t.getID() + (forward ? "" : " REVERSE"));
        out.println(":: " 
            + (forward ? "" : "( ") 
            + sourceSet.toPattern(true) 
            + (forward ? "" : " )")
            + " ;");
        out.println("# Unicode: " + sourceSet.toPattern(false));
        out.println();
    }

    static void usage() {
        System.err.println("Usage: ID [ NFD|NFKD [lower] ]");
        System.exit(1);
    }
}
