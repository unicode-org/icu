/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/29/2001  aliu        Creation.
*   06/26/2002  aliu        Moved to com.ibm.icu.dev.tool.translit
**********************************************************************
*/
package com.ibm.icu.dev.tool.translit;
import java.util.*;
import com.ibm.icu.dev.tool.translit.UnicodeSetClosure;
import java.io.*;
import com.ibm.icu.text.*;

/**
 * Class that generates source set information for a transliterator.
 * 
 * To run, use:
 * 
 *   java com.ibm.icu.dev.tool.translit.SourceSet Latin-Katakana NFD lower
 * 
 * Output is produced in the command console, and a file with more detail is also written.
 * 
 * To see if it works, use:
 * 
 *   java com.ibm.icu.dev.test.translit.TransliteratorTest -v -nothrow TestIncrementalProgress
 * 
 * and
 *  
 *   java com.ibm.icu.dev.demo.translit.Demo
 */
public class SourceSet {

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
