/*
**********************************************************************
*   Copyright (c) 2001-2007, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/29/2001  aliu        Creation.
**********************************************************************
*/
package com.ibm.icu.dev.tool.translit;
import com.ibm.icu.text.*;

/**
 * Adjunct class to getIndexFilters.bat.  Just generates source sets
 * and their closures.
 *
 * Usage: ID [ NFD|NFKD [lower] ]
 *
 *   java -classpath classes com.ibm.icu.dev.tool.translit.genIndexFilters
 *                              Latin-Greek NFD lower
 *
 * The 'NFD'|'NFKD' and 'lower' args are optional, but 'lower' can
 * only be specified if 'NFD' or 'NFKD' is.
 *
 * DO NOT CHANGE OUTPUT FORMAT.  This tool's output is read by a Perl
 * script.
 */
public class genIndexFilters {

    public static void main(String[] args) {
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

    static void showSourceSet(String ID, Normalizer.Mode m, boolean lowerFirst) {
        Transliterator t = Transliterator.getInstance(ID);
        UnicodeSet sourceSet = t.getSourceSet();
        if (m != Normalizer.NONE || lowerFirst) {
            UnicodeSetClosure.close(sourceSet, m, lowerFirst);
        }
        System.out.println(sourceSet.toPattern(true));
    }

    static void usage() {
        System.err.println("Usage: ID [ NFD|NFKD [lower] ]");
        System.exit(1);
    }
}
