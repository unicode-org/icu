/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/29/2001  aliu        Creation.
**********************************************************************
*/
package com.ibm.text;
import java.util.*;
import com.ibm.tools.translit.UnicodeSetClosure;

/**
 * This is a small class that resides in the com.ibm.text package in
 * order to access some package-private API.  It is used for
 * development purposes and should be ignored by end clients.
 */
public class TransliteratorUtility {

    public static void main(String[] args) {
        if (args.length == 0) {
            // Compute and display the source sets for all system
            // transliterators.
            for (Enumeration e = Transliterator.getAvailableIDs(); e.hasMoreElements(); ) {
                String ID = (String) e.nextElement();
                showSourceSet(ID, Normalizer.NO_OP, false);
            }
        } else {
            // Usage: ID [NFKD | NFD] [lower]
            Normalizer.Mode m = Normalizer.NO_OP;
            boolean lowerFirst = false;
            if (args.length >= 2) {
                if (args[1].equalsIgnoreCase("NFD")) {
                    m = Normalizer.DECOMP;
                } else if (args[1].equalsIgnoreCase("NFKD")) {
                    m = Normalizer.DECOMP_COMPAT;
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

    static void showSourceSet(String ID, Normalizer.Mode m, boolean lowerFirst) {
        Transliterator t = Transliterator.getInstance(ID);
        UnicodeSet sourceSet = t.getSourceSet();
        if (m != Normalizer.NO_OP || lowerFirst) {
            UnicodeSetClosure.close(sourceSet, m, lowerFirst);
        }
        System.out.println(t.getID() + ": " +
                           sourceSet.toPattern(true));
    }

    static void usage() {
        System.err.println("Usage: ID [ NFD|NFKD [lower] ]");
        System.exit(1);
    }
}
