/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/normalizer/TestCanonicalIterator.java,v $ 
 * $Date: 2002/02/01 02:05:35 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.test.normalizer;

import com.ibm.test.*;
import com.ibm.text.*;
import com.ibm.util.Utility;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;

// TODO: fit into test framework

public class TestCanonicalIterator {
    static final String testArray[] = {
        "Åd\u0307\u0327",
        "\u010d\u017E",
        "x\u0307\u0327",
    };

    public static void main(String[] args) {
        // set up for readable display
        Transliterator name = Transliterator.getInstance("name");
        Transliterator hex = Transliterator.getInstance("hex");
        
        // check build
        UnicodeSet ss = CanonicalIterator.getSafeStart();
        System.out.println("Safe Start: " + ss.toPattern(true));
        System.out.println();
        ss = CanonicalIterator.getStarts('a');
        System.out.println("Characters with 'a' at the start of their decomposition: " + ss.toPattern(true));
        
        // check permute
        System.out.println(collectionToString(CanonicalIterator.permute("ABC")));
        
        // try samples
        for (int i = 0; i < testArray.length; ++i) {
            System.out.println();
            System.out.println("Results for: " + name.transliterate(testArray[i]));
            CanonicalIterator it = new CanonicalIterator(testArray[i]);
            int counter = 0;
            while (true) {
                String result = it.next();
                if (result == null) break;
                System.out.println(++counter + ": " + hex.transliterate(result));
                System.out.println(" = " + name.transliterate(result));
            }
        }
    }
    
    static String collectionToString(Collection col) {
        StringBuffer result = new StringBuffer();
        Iterator it = col.iterator();
        while (it.hasNext()) {
            if (result.length() != 0) result.append(", ");
            result.append(it.next().toString());
        }
        return result.toString();
    }
}