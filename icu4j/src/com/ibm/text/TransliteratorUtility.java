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

/**
 * This is a small class that resides in the com.ibm.text package in
 * order to access some package-private API.  It is used for
 * development purposes and should be ignored by end clients.
 */
public class TransliteratorUtility {

    public static void main(String[] args) {
        // Compute and display the source sets for all system
        // transliterators.
        for (Enumeration e = Transliterator.getAvailableIDs(); e.hasMoreElements(); ) {
            String ID = (String) e.nextElement();
            Transliterator t = Transliterator.getInstance(ID);
            UnicodeSet sourceSet = t.getSourceSet();
            System.out.println(t.getID() + ": " +
                               sourceSet.toPattern(true));
        }
    }
}
